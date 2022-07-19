package com.personio.synthetics.step.assertion

import com.datadog.api.v1.client.model.SyntheticsStep
import com.datadog.api.v1.client.model.SyntheticsStepType
import com.personio.synthetics.client.BrowserTest
import com.personio.synthetics.model.assertion.AssertionParams
import com.personio.synthetics.model.assertion.DownloadedFileAssertionParams
import com.personio.synthetics.model.assertion.File
import com.personio.synthetics.model.assertion.FileNameCheckType
import com.personio.synthetics.model.assertion.FileSizeCheckType
import com.personio.synthetics.model.assertion.NameCheck
import com.personio.synthetics.model.assertion.SizeCheck
import com.personio.synthetics.step.addStep
import com.personio.synthetics.step.withParamType
import org.intellij.lang.annotations.Language

/**
 * Adds a new custom Javascript assertion step to the synthetic browser test
 * @param stepName Name of the step
 * @param code The javascript code to be executed for performing the assertion
 * @param f Additional configurations that need to be added to the step like timeout, allowFailure etc.
 * @return Custom javascript assertion type synthetic step object
 */
fun BrowserTest.customJavascriptAssertion(
    stepName: String,
    @Language("JS") code: String,
    f: (SyntheticsStep.() -> Unit)? = null
) = addStep(stepName) {
    type = SyntheticsStepType.ASSERT_FROM_JAVASCRIPT
    params = AssertionParams(
        code = code
    )
    if (f != null) f()
}

/**
 * Adds a new downloaded file assertion step to the synthetic browser test
 * @param stepName Name of the step
 * @param f Add all the parameters required for this test step
 * @return SyntheticsCommonAssertionSteps object with the downloaded file assertion step added
 */
fun BrowserTest.downloadedFileAssertion(stepName: String, f: DownloadedFileAssertionStep.() -> Unit): DownloadedFileAssertionStep =
    addStep(stepName, DownloadedFileAssertionStep()) {
        type = SyntheticsStepType.ASSERT_FILE_DOWNLOAD
        params = DownloadedFileAssertionParams(File())
        f()
    }

/**
 * Configure the Downloaded file assertion step for the synthetic browser test
 */
class DownloadedFileAssertionStep : SyntheticsStep() {
    /**
     * Sets the file name to be checked for the Downloaded file assertion step
     * @param checkType The type of check to be done on the name of the downloaded file
     * @param value Expected name for the Downloaded file
     * @return DownloadedFileAssertionStep object with file.nameCheck param set
     */
    fun nameCheck(checkType: FileNameCheckType, value: String = "") = apply {
        if (checkType !in listOf(FileNameCheckType.IS_EMPTY, FileNameCheckType.NOT_IS_EMPTY)) {
            check(!value.isEmpty()) { "Expected value is a required parameter for the file name check in the step '${this.name}' when the passed check type is $checkType" }
        }
        params = withParamType<DownloadedFileAssertionParams> {
            copy(file = file.copy(nameCheck = NameCheck(checkType.value.toString(), value)))
        }
    }

    /**
     * Sets the file size to be checked for the Downloaded file assertion step
     * @param checkType Check type for the file size check of the Downloaded file assertion step
     * @param value Expected file size in KB for the Downloaded file assertion step
     * @return DownloadedFileAssertionStep object with file.sizeCheck param set
     */
    fun sizeCheck(checkType: FileSizeCheckType, value: Int) = apply {
        params = withParamType<DownloadedFileAssertionParams> {
            copy(file = file.copy(sizeCheck = SizeCheck(checkType.value.toString(), value)))
        }
    }

    /**
     * Sets the file MD5 value to be checked for the Downloaded file assertion step
     * @param value Expected MD5 value for the Downloaded file assertion step
     * @return DownloadedFileAssertionStep object with file.md5 param set
     */
    fun expectedMd5(value: String) = apply {
        params = withParamType<DownloadedFileAssertionParams> {
            copy(file = file.copy(md5 = value))
        }
    }
}
