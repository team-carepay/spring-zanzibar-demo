# Spring Security goes to Zanzibar

In recent years, more and more companies adopt a new authorization framework
based on the Google Zanzibar Paper: a highly insightful document that details
Google's approach to scalable and efficient authorization.

While Spring Security provides support for performing Role Based Access Control (RBAC),
if you're running a multi-tenant system it is left to the developers to implement proper
Data Access Control. 

In this talk, we are going to explore the concepts of relation based authorization models.
We will show a hands-on example using Spring Security and SpiceDB, where we provide a way
how you can implement a scalable, flexible, efficient and reliable authorization model
for your Spring Boot applications.

## Technologies used

* Spring Boot
* Docker compose
* SpiceDB
* Kafka
