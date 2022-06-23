package com.personio.synthetics.step.api

import com.datadog.api.v1.client.model.HTTPMethod
import com.datadog.api.v1.client.model.SyntheticsAssertion
import com.datadog.api.v1.client.model.SyntheticsAssertionTarget
import com.datadog.api.v1.client.model.SyntheticsGlobalVariableParseTestOptionsType
import com.datadog.api.v1.client.model.SyntheticsGlobalVariableParserType
import com.datadog.api.v1.client.model.SyntheticsStep
import com.datadog.api.v1.client.model.SyntheticsStepType
import com.datadog.api.v1.client.model.SyntheticsTestRequest
import com.datadog.api.v1.client.model.SyntheticsVariableParser
import com.personio.synthetics.client.BrowserTest
import com.personio.synthetics.model.api.ExtractValue
import com.personio.synthetics.model.api.RequestParams
import com.personio.synthetics.step.addStep
import com.personio.synthetics.step.withParamType
import java.net.URL

/**
 * Adds a new API step to the synthetic browser test
 * @param stepName Name of the step
 * @param f Add all the parameters required for this test step
 * @return ApiStep object with this step added
 */
fun BrowserTest.apiStep(stepName: String, f: ApiStep.() -> Unit): ApiStep =
    addStep(stepName, ApiStep()) {
        type = SyntheticsStepType.RUN_API_TEST
        params = with(RequestParams()) {
            request.config.request(SyntheticsTestRequest().url(config?.request?.url))
            copy(request = request.copy(subtype = "http"))
        }
        f()
    }

/**
 * Configure the API step for the synthetic browser test
 */
class ApiStep : SyntheticsStep() {
    /**
     * Adds a new assertion to the API step
     * @param f Add the assertion required for the API cal
     * type: Add the type of assertion
     * property: Header property where assertion is to be performed (Optional)
     * property parameter is not required for body or status code assertions
     * operator: Operator type
     * expected: Expected value for assertion
     * @return ApiStep object with this assertion added
     */
    fun assertion(f: SyntheticsAssertionTarget.() -> Unit) = apply {
        val assertionTarget = SyntheticsAssertionTarget()
        assertionTarget.f()
        withParamType<RequestParams> {
            apply { request.config.assertions?.add(SyntheticsAssertion(assertionTarget)) }
        }
    }

    /**
     * Body to be passed to the API request
     * @param body The body of the request
     * @return ApiStep object with the request body set
     */
    fun requestBody(body: String) = apply {
        withParamType<RequestParams> {
            apply { request.config.request.body = body }
        }
    }

    /**
     * Headers to be passed to the API request
     * @param headers a map of header name and header values
     * eg: mapOf("content-type" to "application/json")
     * @return ApiStep object with the request headers set
     */
    fun requestHeaders(headers: Map<String, String>) = apply {
        withParamType<RequestParams> {
            apply { request.config.request.headers = headers }
        }
    }

    /**
     * The method of the API request
     * @param method The method of the request GET/POST
     * @return ApiStep object with the method set
     */
    fun method(method: HTTPMethod) = apply {
        withParamType<RequestParams> {
            apply { request.config.request.method = method }
        }
    }

    /**
     * Set the url used for the API request
     * @param url url to be used for the API request
     * Use only the location (eg: /test/page) for appending to the base url of the test
     * else pass full url including http(s)://
     * @return ApiStep object with the method set
     */
    fun url(url: String) = apply {
        withParamType<RequestParams> {
            apply {
                val target = runCatching { URL(url) }
                    .recover { URL(request.config.request.url + url) }
                    .getOrThrow()
                request.config.request.url = target.toString()
            }
        }
    }

    /**
     * Extract the header value from the API response
     * @param name Name of the variable to extract the value to
     * @param field Header field that need to be extracted from the response
     * @param regex Regular expression to extract the value in the header field (Optional parameter)
     * @return ApiStep object with the extract variable step added
     */
    fun extractHeaderValue(name: String, field: String, regex: String? = null) = apply {
        val extractValue = ExtractValue(name)
        val parserType =
            if (regex == null) SyntheticsGlobalVariableParserType.RAW else SyntheticsGlobalVariableParserType.REGEX

        extractValue
            .field(field)
            .parser(
                SyntheticsVariableParser()
                    .type(parserType)
                    .value(regex)
            )
            .type(SyntheticsGlobalVariableParseTestOptionsType.HTTP_HEADER)
        params = withParamType<RequestParams> {
            setExtractValue(extractValue)
        }
    }

    /**
     * Extract value from the response body
     * @param name Name of the variable to extract the value to
     * @param parserType Type of parser to use
     * @param parserValue Parser value (Optional parameter)
     * For REGEX type, pass the regular expression
     * For JSON_PATH type, pass the json path
     * For X_PATH type, pass the xpath
     * For RAW type, this parameter is not required
     * @return ApiStep object with the extract variable step added
     */
    fun extractBodyValue(
        name: String,
        parserType: SyntheticsGlobalVariableParserType,
        parserValue: String? = null
    ) = apply {
        val parser = SyntheticsVariableParser()
            .type(parserType)
            .value(parserValue)

        val newExtractValue = ExtractValue(name)
        newExtractValue
            .parser(parser)
            .type(SyntheticsGlobalVariableParseTestOptionsType.HTTP_BODY)
        params = withParamType<RequestParams> {
            setExtractValue(newExtractValue)
        }
    }

    /**
     * Set extract value object
     * @param extractValue Extract value object that need to be set
     * @return RequestParams object with the extract value object added
     */
    private fun RequestParams.setExtractValue(extractValue: ExtractValue): RequestParams {
        val currentExtractValues = request.options.extract_values.orEmpty()
        return copy(request = request.copy(options = request.options.copy(extract_values = currentExtractValues + extractValue)))
    }
}