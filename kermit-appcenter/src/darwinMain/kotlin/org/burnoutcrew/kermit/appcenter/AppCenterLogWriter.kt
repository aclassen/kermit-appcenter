/*
 * Copyright 2022 André Claßen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.burnoutcrew.kermit.appcenter

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

@ExperimentalKermitApi
actual class AppCenterLogWriter actual constructor(
    private val minSeverity: Severity,
    private val minCrashSeverity: Severity,
    private val printTag: Boolean
) : LogWriter() {

    init {
        assert(minSeverity <= minCrashSeverity) {
            "minSeverity ($minSeverity) cannot be greater than minCrashSeverity ($minCrashSeverity)"
        }
    }

    override fun isLoggable(severity: Severity): Boolean = severity >= minSeverity

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (throwable != null && severity >= minCrashSeverity) {
            val properties = mapOf(
                "message" to if (printTag) {
                    "$tag : $message"
                } else {
                    message
                }
            )
            sendException(throwable, properties)
        }
    }

    private fun sendException(throwable: Throwable, properties: Map<String, String>) {
        @Suppress("UNCHECKED_CAST")
        val props = properties as? Map<Any?, *>
        transformException(throwable) { name, description, stackFrames ->
            MSACCrashes.trackException(
                MSACExceptionModel(name, description, emptyList<String>())
                    .apply {
                        frames = stackFrames
                        stackTrace = throwable.stackTraceToString()
                    },
                props,
                null
            )
        }
    }
}


private fun transformException(t: Throwable, block: (String, String, stackTrace: List<MSACStackFrame>) -> Unit) {
    fun throwableBoilerplate(frameString: String, lookFor: String) =
        !frameString.contains("kotlin.${lookFor}")
                &&
                !frameString.contains("${lookFor}.<init>")

    fun createStackFrameFromString(line: String): MSACStackFrame? {
        // Parse adapted from MSACExceptionModel.m  ¯\_(ツ)_/¯
        return line.split(' ', '-', '[', ']', '+', '?', '.', ',')
            .filter { it.isNotEmpty() }
            .let { frameParts ->
                if (frameParts.size > 5) {
                    val frame = MSACStackFrame()
                    frame.fileName = frameParts[1]
                    frame.address = frameParts[2]
                    frame.className = frameParts[3]
                    frame.methodName = frameParts[4]
                    // Add kotlin class and method name on our own
                    if (frameParts[3].startsWith("kfun:")) {
                        val startIndex = line.indexOf(frameParts[3])
                        val endIndex = line.indexOf(' ', startIndex).let { if (it < 0) line.length else it }
                        val delimiter = line.indexOf('#', startIndex)
                        if (delimiter > 0) {
                            frame.className = line.substring(startIndex, delimiter)
                            frame.methodName = line.substring(delimiter + 1, endIndex)
                        } else {
                            frame.className = line.substring(startIndex, endIndex)
                            frame.methodName = null
                        }
                    }
                    frame
                } else {
                    null
                }
            }
    }

    val stackTrace = t.getStackTrace()
    val index = stackTrace.indexOfFirst { throwableBoilerplate(it, "Exception") || throwableBoilerplate(it, "Throwable") }
    val rewrittenStackTrace = stackTrace.drop(index).mapNotNull { createStackFrameFromString(it) }
    block(
        t::class.simpleName ?: "(Unknown Type)",
        t.message ?: "",
        rewrittenStackTrace
    )
}
