# What smbj and its message bus reach for on a JVM and never on Android: RMI's exception type,
# JavaEE expression language, and the GSS-API. R8 has to be told, or it refuses to finish rather
# than shrink away a reference it cannot resolve.
-dontwarn java.rmi.**
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
