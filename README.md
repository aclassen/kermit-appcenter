# Kermit Crash Logging - AppCenter

With the `kermit-appcenter` module, you can setup kermit to automatically send handled exceptions and crash reports to AppCenter.

```

Setup the `AppCenterLogWriter` with your `Logger`. The constructor for both platforms is the same, so in
shared code, or in platform-specific Kotlin, run the following:

```kotlin
Logger.addLogWriter(AppCenterLogWriter(Severity.Error, Severity.Error, true))
```

([Static Config](../Kermit#local-configuration) would be similar)

On either platform, you should make sure logging is configured immediately after AppCenter is initialized, to avoid
a gap where some other failure may happen but logging is not capturing info.

### iOS

For iOS, besides regular logging, you will also want to configure Kotlin's uncaught exception handling. `kermit-appcenter`
provides the `setupAppCenterExceptionHook` helper function to handle this for you.

If you don't need to make kermit logging calls from Swift/Objective C code, we recommend not exporting Kermit in the 
framework exposed to your iOS app. To setup Kermit configuration you can make a top level helper method in the `iosMain` 
sourceset that you call from Swift code to avoid binary bloat. The same rule of thumb applies to `kermit-appcenter` and 
since the added api is only needed for configuration, a Kotlin helper method is almost always the best option. Here is a basic example.

```kotlin
// in Kermit/AppInit.kt
fun setupKermit() {
    Logger.addLogWriter(AppCenterLogWriter(Severity.Error, Severity.Error, true))
    setupAppCenterExceptionHook(Logger)
}
```

```swift
// in AppDelegate.swift
@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    ...
    func application(
        _ application: UIApplication, 
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Note: This MUST be the first two statement, in this order, for Kermit and AppCenter
        // to handle any crashes in your app launch. 
        // If the app crashes before these calls run, it will not show up properly in the dashboard
        AppCenter.start(withAppSecret: "API SECRET", services: [Crashes.self])
        AppInitKt.setupKermit()
        //...
    }
}
```

If providing instances built from a base `Logger` via DI, you need to make sure that the `setupAppCenterExceptionHook` 
call happens immediately on app launch, not lazily inside a lambda given to your DI framework. 
Example for Koin: 
```kotlin
// in iosMain
fun initKoinIos() {
    val baseLogger = Logger(StaticConfig(logWriterList = listOf(platformLogWriter(), AppCenterLogWriter())))
    // Note that this runs sequentially, not in the lambda pass to the module function
    setupAppCenterExceptionHook(log)

    return initKoin(
        module { 
            factory { (tag: String?) -> if (tag != null) baseLogger.withTag(tag) else baseLogger }
        }
    )
}
```

```swift
// in AppDelegate.swift
@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    ...
    func application(
        _ application: UIApplication, 
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Note: This MUST be the first two statement, in this order, for Kermit and AppCenter
        // to handle any crashes in your app launch. 
        // If the app crashes before these calls run, it will not show up properly in the dashboard
        AppCenter.start(withAppSecret: "API SECRET", services: [Crashes.self])
        MyKoinKt.initKoinIos()
        //...
    }
}
```


# Reading iOS Crash Logs
When a crash occurs in Kotlin code, the stack trace in AppCenter gets lost at the Swift-Kotlin barrier, which can make it
difficult to determine the root cause of a crash that happens in Kotlin. 


To remedy this, `kermit-appcenter` reports unhandled Kotlin exceptions as separate, non-fatal exceptions, which will show up in AppCenter with a readable stack trace.