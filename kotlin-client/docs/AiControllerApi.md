# AiControllerApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**getClientsStatus**](AiControllerApi.md#getClientsStatus) | **GET** /api/ai/clients/status |  |
| [**getStatus**](AiControllerApi.md#getStatus) | **GET** /api/ai/status |  |
| [**workflow**](AiControllerApi.md#workflow) | **POST** /api/ai/workflow |  |
| [**workflowWithClient**](AiControllerApi.md#workflowWithClient) | **POST** /api/ai/workflow/{clientType} |  |


<a id="getClientsStatus"></a>
# **getClientsStatus**
> kotlin.collections.Map&lt;kotlin.String, kotlin.Any&gt; getClientsStatus()



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = AiControllerApi()
try {
    val result : kotlin.collections.Map<kotlin.String, kotlin.Any> = apiInstance.getClientsStatus()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AiControllerApi#getClientsStatus")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AiControllerApi#getClientsStatus")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**kotlin.collections.Map&lt;kotlin.String, kotlin.Any&gt;**](kotlin.Any.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="getStatus"></a>
# **getStatus**
> kotlin.collections.Map&lt;kotlin.String, kotlin.Any&gt; getStatus()



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = AiControllerApi()
try {
    val result : kotlin.collections.Map<kotlin.String, kotlin.Any> = apiInstance.getStatus()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AiControllerApi#getStatus")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AiControllerApi#getStatus")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**kotlin.collections.Map&lt;kotlin.String, kotlin.Any&gt;**](kotlin.Any.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="workflow"></a>
# **workflow**
> WorkflowResponse workflow(workflowRequest)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = AiControllerApi()
val workflowRequest : WorkflowRequest =  // WorkflowRequest | 
try {
    val result : WorkflowResponse = apiInstance.workflow(workflowRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AiControllerApi#workflow")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AiControllerApi#workflow")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **workflowRequest** | [**WorkflowRequest**](WorkflowRequest.md)|  | |

### Return type

[**WorkflowResponse**](WorkflowResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a id="workflowWithClient"></a>
# **workflowWithClient**
> WorkflowResponse workflowWithClient(clientType, workflowRequest)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = AiControllerApi()
val clientType : kotlin.String = clientType_example // kotlin.String | 
val workflowRequest : WorkflowRequest =  // WorkflowRequest | 
try {
    val result : WorkflowResponse = apiInstance.workflowWithClient(clientType, workflowRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AiControllerApi#workflowWithClient")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AiControllerApi#workflowWithClient")
    e.printStackTrace()
}
```

### Parameters
| **clientType** | **kotlin.String**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **workflowRequest** | [**WorkflowRequest**](WorkflowRequest.md)|  | |

### Return type

[**WorkflowResponse**](WorkflowResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

