# The television shell shrinks by the same rules the phone does, and needs them for the same
# reason: it speaks to shares through smbj, which reaches for three things that exist on a desktop
# JVM and nowhere on Android -- RMI's exception type, JavaEE's expression language, and the
# GSS-API. R8 will not finish while a reference it cannot resolve is left unexplained, so it is
# told here rather than left to fail the build.
-dontwarn java.rmi.**
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
