# AccessChecker — intentionally unobfuscated
# This is a security audit tool. The code should always be fully auditable.

# Never obfuscate anything
-dontobfuscate

# Never shrink anything
-dontshrink

# Never optimise bytecode
-dontoptimize

# Keep source file names and line numbers so stack traces and decompiled
# output map cleanly back to the original source
-keepattributes SourceFile,LineNumberTable,LocalVariableTable,LocalVariableTypeTable

# Keep all class/method/field names exactly as written
-keepnames class ** { *; }

# Keep all annotations (used by Shizuku and AndroidX)
-keepattributes *Annotation*,Signature,Exceptions,InnerClasses,EnclosingMethod
