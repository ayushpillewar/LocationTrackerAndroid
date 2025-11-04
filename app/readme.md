how to kill the emulator process
adb -s emulator-5554 emu kill


The following are some examples of keys you can use in your query:

tag: Matches against the tag field of the log entry.
package: Matches against the package name of the logging app.
process: Matches against the process name of the logging app.
message: Matches against the message part of the log entry.
level: Matches the specified or higher severe log level–for example, DEBUG.
age: Matches if the entry timestamp is recent. Values are specified as a number followed by a letter specifying the time unit: s for seconds, m for minutes, h for hours and d for days. For example, age: 5m filters only messages that were logged in the last 5 minutes.