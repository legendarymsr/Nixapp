/*
 * native_checks.c — low-level security checks that bypass Zygisk Java hooks.
 *
 * Zygisk intercepts Java methods (Runtime.exec, SystemProperties, etc.) but
 * cannot intercept raw Linux syscalls made from a native library loaded before
 * Zygisk's injected code runs. All checks here use only POSIX/Linux syscalls.
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <android/log.h>

#define TAG "AccessCheckerNative"

/* ── File pattern scanner ─────────────────────────────────────────────────── */

/* Returns number of matching lines. Appends them to out_buf if non-NULL. */
static int scan_file_for_patterns(const char *filepath,
                                   const char **patterns, int count,
                                   char *out_buf, size_t buf_size) {
    FILE *f = fopen(filepath, "r");
    if (!f) return 0;

    char line[1024];
    int found = 0;
    while (fgets(line, sizeof(line), f)) {
        for (int i = 0; i < count; i++) {
            if (strstr(line, patterns[i])) {
                found++;
                if (out_buf) {
                    size_t cur = strlen(out_buf);
                    size_t rem = buf_size - cur - 1;
                    if (rem > 0) {
                        strncat(out_buf, line, rem);
                        out_buf[buf_size - 1] = '\0';
                    }
                }
                break;
            }
        }
    }
    fclose(f);
    return found;
}

/* ── Abstract Unix socket probe ───────────────────────────────────────────── */

/* Returns 1 if an abstract socket with this name is bound (i.e. something is
 * listening on it). ENOENT → not bound. ECONNREFUSED / EINPROGRESS → bound. */
static int abstract_socket_exists(const char *name) {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) return 0;

    int flags = fcntl(fd, F_GETFL, 0);
    fcntl(fd, F_SETFL, flags | O_NONBLOCK);

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    size_t nlen = strlen(name);
    if (nlen == 0 || nlen >= sizeof(addr.sun_path) - 1) { close(fd); return 0; }
    memcpy(addr.sun_path + 1, name, nlen);
    socklen_t len = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + 1 + nlen);

    int ret = connect(fd, (struct sockaddr *)&addr, len);
    int err = errno;
    close(fd);

    return (ret == 0 || err == EINPROGRESS || err == ECONNREFUSED) ? 1 : 0;
}

/* ══════════════════════════════════════════════════════════════════════════
   JNI exports — all static methods on com.accesschecker.NativeChecker
   ══════════════════════════════════════════════════════════════════════════ */

/* --- Magisk abstract socket ------------------------------------------------ */
JNIEXPORT jboolean JNICALL
Java_com_accesschecker_NativeChecker_hasMagiskSocket(JNIEnv *env, jclass cls) {
    const char *names[] = { "magisk_service", "magiskd", "magisk_daemon" };
    for (int i = 0; i < 3; i++) {
        if (abstract_socket_exists(names[i])) return JNI_TRUE;
    }
    return JNI_FALSE;
}

/* --- Suspicious mounts (/proc/self/mounts + /proc/mounts) ----------------- */
JNIEXPORT jboolean JNICALL
Java_com_accesschecker_NativeChecker_hasMagiskMounts(JNIEnv *env, jclass cls) {
    const char *pats[] = { "magisk", "magisktmp", ".magisk" };
    if (scan_file_for_patterns("/proc/self/mounts", pats, 3, NULL, 0)) return JNI_TRUE;
    if (scan_file_for_patterns("/proc/mounts",      pats, 3, NULL, 0)) return JNI_TRUE;
    return JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_accesschecker_NativeChecker_getSuspiciousMounts(JNIEnv *env, jclass cls) {
    const char *pats[] = { "magisk", "magisktmp", ".magisk" };
    char buf[4096] = {0};
    scan_file_for_patterns("/proc/self/mounts", pats, 3, buf, sizeof(buf));
    scan_file_for_patterns("/proc/mounts",      pats, 3, buf, sizeof(buf));
    return (*env)->NewStringUTF(env, buf);
}

/* --- /proc/self/maps for injected libraries -------------------------------- */
JNIEXPORT jboolean JNICALL
Java_com_accesschecker_NativeChecker_hasZygiskInMaps(JNIEnv *env, jclass cls) {
    const char *pats[] = { "zygisk", "magisk", "riru", "shamiko", "lspd", "lsposed" };
    return scan_file_for_patterns("/proc/self/maps", pats, 6, NULL, 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_accesschecker_NativeChecker_getMapsMatches(JNIEnv *env, jclass cls) {
    const char *pats[] = { "zygisk", "riru", "shamiko", "lspd", "lsposed" };
    char buf[8192] = {0};
    scan_file_for_patterns("/proc/self/maps", pats, 5, buf, sizeof(buf));
    return (*env)->NewStringUTF(env, buf);
}

/* --- /proc/1/maps (init process — requires no special perms on most kernels) */
JNIEXPORT jboolean JNICALL
Java_com_accesschecker_NativeChecker_hasProc1MagiskMaps(JNIEnv *env, jclass cls) {
    const char *pats[] = { "magisk", "zygisk" };
    return scan_file_for_patterns("/proc/1/maps", pats, 2, NULL, 0) ? JNI_TRUE : JNI_FALSE;
}

/* --- TracerPid (anti-debugging / ptrace detection) ------------------------- */
JNIEXPORT jint JNICALL
Java_com_accesschecker_NativeChecker_getTracerPid(JNIEnv *env, jclass cls) {
    FILE *f = fopen("/proc/self/status", "r");
    if (!f) return -1;

    char line[256];
    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            fclose(f);
            return atoi(line + 10);
        }
    }
    fclose(f);
    return -1;
}

/* --- KernelSU VFS nodes ---------------------------------------------------- */
JNIEXPORT jboolean JNICALL
Java_com_accesschecker_NativeChecker_hasKernelSU(JNIEnv *env, jclass cls) {
    const char *paths[] = {
        "/sys/kernel/su",
        "/sys/kernel/su/version",
        "/proc/kernelsu",
        "/dev/ksud"
    };
    for (int i = 0; i < 4; i++) {
        if (access(paths[i], F_OK) == 0) return JNI_TRUE;
    }
    return JNI_FALSE;
}

/* --- APatch VFS nodes ------------------------------------------------------ */
JNIEXPORT jboolean JNICALL
Java_com_accesschecker_NativeChecker_hasAPatch(JNIEnv *env, jclass cls) {
    const char *paths[] = {
        "/proc/apd",
        "/data/adb/apatch",
        "/dev/apd"
    };
    for (int i = 0; i < 3; i++) {
        if (access(paths[i], F_OK) == 0) return JNI_TRUE;
    }
    return JNI_FALSE;
}

/* --- FUSE filesystem presence (Magisk bind-mounts use FUSE) ---------------- */
JNIEXPORT jboolean JNICALL
Java_com_accesschecker_NativeChecker_hasFuseFilesystem(JNIEnv *env, jclass cls) {
    const char *pats[] = { "fuse" };
    return scan_file_for_patterns("/proc/filesystems", pats, 1, NULL, 0) ? JNI_TRUE : JNI_FALSE;
}

/* --- Native su exec (fork+execve bypasses Zygisk Java hooks) --------------- */
JNIEXPORT jboolean JNICALL
Java_com_accesschecker_NativeChecker_nativeSuExec(JNIEnv *env, jclass cls) {
    int pipefd[2];
    if (pipe(pipefd) < 0) return JNI_FALSE;

    pid_t pid = fork();
    if (pid < 0) {
        close(pipefd[0]);
        close(pipefd[1]);
        return JNI_FALSE;
    }

    if (pid == 0) {
        /* Child: redirect stdout to pipe, silence stdin/stderr */
        close(pipefd[0]);
        dup2(pipefd[1], STDOUT_FILENO);
        close(pipefd[1]);

        int devnull = open("/dev/null", O_RDWR);
        if (devnull >= 0) {
            dup2(devnull, STDIN_FILENO);
            dup2(devnull, STDERR_FILENO);
            close(devnull);
        }

        char *const argv[] = { "su", "-c", "id", NULL };
        char *const envp[] = { "PATH=/system/bin:/sbin:/su/bin:/system/xbin:/data/local/xbin", NULL };

        const char *su_paths[] = {
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/su/bin/su", "/data/local/xbin/su"
        };
        for (int i = 0; i < 5; i++) {
            execve(su_paths[i], argv, envp);
        }
        _exit(127);
    }

    /* Parent: read child's stdout */
    close(pipefd[1]);
    char buf[256] = {0};
    ssize_t n = read(pipefd[0], buf, sizeof(buf) - 1);
    close(pipefd[0]);

    int status = 0;
    waitpid(pid, &status, 0);

    return (n > 0 && strstr(buf, "uid=0") != NULL) ? JNI_TRUE : JNI_FALSE;
}
