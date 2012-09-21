# camel-salesforce

Camel Salesforce component

This component supports producer and consumer endpoints to communicate with Salesforce using Java DTOs. 
There is a companion maven plugin [camel-salesforce-plugin](https://github.com/dhirajsb/camel-salesforce-maven-plugin) that generates these DTOs. 

The component supports the following Salesforce APIs

## REST API ##

Producer endpoints can use the following APIs. Most of the APIs process one record at a time, the Query API can retrieve multiple Records. 

* getVersions - Gets supported Salesforce REST API versions
* getResources - Gets available Salesforce REST Resource endpoints
* getGlobalObjects - Gets metadata for all available SObject types
* getBasicInfo - Gets basic metadata for a specific SObject type
* getDescription - Gets comprehensive metadata for a specific SObject type
* getSObject - Gets an SObject using its Salesforce Id
* createSObject - Creates an SObject
* updateSObject - Updates an SObject using Id
* deleteSObject - Deletes an SObject using Id
* getSObjectWithId - Gets an SObject using an external (user defined) id field
* upsertSObject - Updates or inserts an SObject using an external id
* deleteSObjectWithId - Deletes an SObject using an external id
* query - Runs a Salesforce SOQL query
* queryMore - Retrieves more results (in case of large number of results) using result link returned from the 'query' API
* search - Runs a Salesforce SOSL query

## Bulk API ##

Producer endpoints can use the following APIs. All Job data formats, i.e. xml, csv, zip/xml, and zip/csv are supported. 
The request and response have to be marshalled/unmarshalled by the route. Usually the request will be some stream source like a CSV file, 
and the response may also be saved to a file to be correlated with the request. 

* createJob - Creates a Salesforce Bulk Job
* getJob - Gets a Job using its Salesforce Id
* closeJob - Closes a Job
* abortJob - Aborts a Job
* createBatch - Submits a Batch within a Bulk Job
* getBatch - Gets a Batch using Id
* getAllBatches - Gets all Batches for a Bulk Job Id
* getRequest - Gets Request data (XML/CSV) for a Batch
* getResults - Gets the results of the Batch when its complete
* createBatchQuery - Creates a Batch from an SOQL query
* getQueryResultIds - Gets a list of Result Ids for a Batch Query
* getQueryResult - Gets results for a Result Id

## Streaming API ##

Consumer endpoints can use the following sytax for streaming endpoints to receive Salesforce notifications on create/update. 

* To create a topic
	from("force:CamelTestTopic?notifyForFields=ALL&notifyForOperations=ALL&" +
		sObjectName=Merchandise__c&" +
		updateTopic=true&sObjectQuery=SELECT Id, Name FROM Merchandise__c").
		...

* To subscribe to an existing topic
	from("force:CamelTestTopic").
		...
