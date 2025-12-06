# TaskControllerApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createTask**](TaskControllerApi.md#createTask) | **POST** /api/tasks |  |
| [**deleteTask**](TaskControllerApi.md#deleteTask) | **DELETE** /api/tasks/{id} |  |
| [**disableTaskReminder**](TaskControllerApi.md#disableTaskReminder) | **PUT** /api/tasks/{id}/disable-reminder |  |
| [**enableTaskReminder**](TaskControllerApi.md#enableTaskReminder) | **PUT** /api/tasks/{id}/enable-reminder |  |
| [**getAllTasks**](TaskControllerApi.md#getAllTasks) | **GET** /api/tasks |  |
| [**getTaskById**](TaskControllerApi.md#getTaskById) | **GET** /api/tasks/{id} |  |
| [**getTaskStatistics**](TaskControllerApi.md#getTaskStatistics) | **GET** /api/tasks/statistics |  |
| [**getTasksByCompleted**](TaskControllerApi.md#getTasksByCompleted) | **GET** /api/tasks/completed/{isCompleted} |  |
| [**getTasksByDateRange**](TaskControllerApi.md#getTasksByDateRange) | **GET** /api/tasks/date-range |  |
| [**getTasksByLocation**](TaskControllerApi.md#getTasksByLocation) | **GET** /api/tasks/location |  |
| [**markTaskAsCompleted**](TaskControllerApi.md#markTaskAsCompleted) | **PUT** /api/tasks/{id}/complete |  |
| [**markTaskAsUncompleted**](TaskControllerApi.md#markTaskAsUncompleted) | **PUT** /api/tasks/{id}/uncomplete |  |
| [**searchTasksByTitle**](TaskControllerApi.md#searchTasksByTitle) | **GET** /api/tasks/search |  |
| [**toggleTaskCompletion**](TaskControllerApi.md#toggleTaskCompletion) | **PUT** /api/tasks/{id}/toggle |  |
| [**updateTask**](TaskControllerApi.md#updateTask) | **PUT** /api/tasks/{id} |  |


<a id="createTask"></a>
# **createTask**
> Task createTask(task)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val task : Task =  // Task | 
try {
    val result : Task = apiInstance.createTask(task)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#createTask")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#createTask")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **task** | [**Task**](Task.md)|  | |

### Return type

[**Task**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a id="deleteTask"></a>
# **deleteTask**
> kotlin.Boolean deleteTask(id)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val id : kotlin.Long = 789 // kotlin.Long | 
try {
    val result : kotlin.Boolean = apiInstance.deleteTask(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#deleteTask")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#deleteTask")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Long**|  | |

### Return type

**kotlin.Boolean**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="disableTaskReminder"></a>
# **disableTaskReminder**
> kotlin.Boolean disableTaskReminder(id)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val id : kotlin.Long = 789 // kotlin.Long | 
try {
    val result : kotlin.Boolean = apiInstance.disableTaskReminder(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#disableTaskReminder")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#disableTaskReminder")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Long**|  | |

### Return type

**kotlin.Boolean**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="enableTaskReminder"></a>
# **enableTaskReminder**
> kotlin.Boolean enableTaskReminder(id)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val id : kotlin.Long = 789 // kotlin.Long | 
try {
    val result : kotlin.Boolean = apiInstance.enableTaskReminder(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#enableTaskReminder")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#enableTaskReminder")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Long**|  | |

### Return type

**kotlin.Boolean**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="getAllTasks"></a>
# **getAllTasks**
> kotlin.collections.List&lt;Task&gt; getAllTasks()



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
try {
    val result : kotlin.collections.List<Task> = apiInstance.getAllTasks()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#getAllTasks")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#getAllTasks")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**kotlin.collections.List&lt;Task&gt;**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="getTaskById"></a>
# **getTaskById**
> Task getTaskById(id)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val id : kotlin.Long = 789 // kotlin.Long | 
try {
    val result : Task = apiInstance.getTaskById(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#getTaskById")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#getTaskById")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Long**|  | |

### Return type

[**Task**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="getTaskStatistics"></a>
# **getTaskStatistics**
> kotlin.collections.Map&lt;kotlin.String, kotlin.Long&gt; getTaskStatistics()



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
try {
    val result : kotlin.collections.Map<kotlin.String, kotlin.Long> = apiInstance.getTaskStatistics()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#getTaskStatistics")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#getTaskStatistics")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**kotlin.collections.Map&lt;kotlin.String, kotlin.Long&gt;**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="getTasksByCompleted"></a>
# **getTasksByCompleted**
> kotlin.collections.List&lt;Task&gt; getTasksByCompleted(isCompleted)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val isCompleted : kotlin.Boolean = true // kotlin.Boolean | 
try {
    val result : kotlin.collections.List<Task> = apiInstance.getTasksByCompleted(isCompleted)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#getTasksByCompleted")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#getTasksByCompleted")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **isCompleted** | **kotlin.Boolean**|  | |

### Return type

[**kotlin.collections.List&lt;Task&gt;**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="getTasksByDateRange"></a>
# **getTasksByDateRange**
> kotlin.collections.List&lt;Task&gt; getTasksByDateRange(startDate, endDate)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val startDate : kotlin.Long = 789 // kotlin.Long | 
val endDate : kotlin.Long = 789 // kotlin.Long | 
try {
    val result : kotlin.collections.List<Task> = apiInstance.getTasksByDateRange(startDate, endDate)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#getTasksByDateRange")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#getTasksByDateRange")
    e.printStackTrace()
}
```

### Parameters
| **startDate** | **kotlin.Long**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **endDate** | **kotlin.Long**|  | |

### Return type

[**kotlin.collections.List&lt;Task&gt;**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="getTasksByLocation"></a>
# **getTasksByLocation**
> kotlin.collections.List&lt;Task&gt; getTasksByLocation(latitude, longitude, radius)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val latitude : kotlin.Double = 1.2 // kotlin.Double | 
val longitude : kotlin.Double = 1.2 // kotlin.Double | 
val radius : kotlin.Double = 1.2 // kotlin.Double | 
try {
    val result : kotlin.collections.List<Task> = apiInstance.getTasksByLocation(latitude, longitude, radius)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#getTasksByLocation")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#getTasksByLocation")
    e.printStackTrace()
}
```

### Parameters
| **latitude** | **kotlin.Double**|  | |
| **longitude** | **kotlin.Double**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **radius** | **kotlin.Double**|  | |

### Return type

[**kotlin.collections.List&lt;Task&gt;**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="markTaskAsCompleted"></a>
# **markTaskAsCompleted**
> kotlin.Boolean markTaskAsCompleted(id)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val id : kotlin.Long = 789 // kotlin.Long | 
try {
    val result : kotlin.Boolean = apiInstance.markTaskAsCompleted(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#markTaskAsCompleted")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#markTaskAsCompleted")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Long**|  | |

### Return type

**kotlin.Boolean**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="markTaskAsUncompleted"></a>
# **markTaskAsUncompleted**
> kotlin.Boolean markTaskAsUncompleted(id)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val id : kotlin.Long = 789 // kotlin.Long | 
try {
    val result : kotlin.Boolean = apiInstance.markTaskAsUncompleted(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#markTaskAsUncompleted")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#markTaskAsUncompleted")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Long**|  | |

### Return type

**kotlin.Boolean**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="searchTasksByTitle"></a>
# **searchTasksByTitle**
> kotlin.collections.List&lt;Task&gt; searchTasksByTitle(title)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val title : kotlin.String = title_example // kotlin.String | 
try {
    val result : kotlin.collections.List<Task> = apiInstance.searchTasksByTitle(title)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#searchTasksByTitle")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#searchTasksByTitle")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **title** | **kotlin.String**|  | |

### Return type

[**kotlin.collections.List&lt;Task&gt;**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="toggleTaskCompletion"></a>
# **toggleTaskCompletion**
> kotlin.Boolean toggleTaskCompletion(id)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val id : kotlin.Long = 789 // kotlin.Long | 
try {
    val result : kotlin.Boolean = apiInstance.toggleTaskCompletion(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#toggleTaskCompletion")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#toggleTaskCompletion")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Long**|  | |

### Return type

**kotlin.Boolean**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="updateTask"></a>
# **updateTask**
> Task updateTask(id, task)



### Example
```kotlin
// Import classes:
//import org.openapitools.client.infrastructure.*
//import org.openapitools.client.models.*

val apiInstance = TaskControllerApi()
val id : kotlin.Long = 789 // kotlin.Long | 
val task : Task =  // Task | 
try {
    val result : Task = apiInstance.updateTask(id, task)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaskControllerApi#updateTask")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaskControllerApi#updateTask")
    e.printStackTrace()
}
```

### Parameters
| **id** | **kotlin.Long**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **task** | [**Task**](Task.md)|  | |

### Return type

[**Task**](Task.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

