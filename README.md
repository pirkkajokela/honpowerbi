This is a prototype app for loading HappyOrNot data to Power BI
===============================================================

How to register this app to Power BI?
https://powerbi.microsoft.com/en-us/documentation/powerbi-developer-register-a-web-app/#register-a-web-app-with-power-bi-app-registration-tool

How to authenticate to Azure AD?
https://msdn.microsoft.com/fi-fi/library/azure/dn645543.aspx
(These instructions do not use Microsoft .NET classes.)

Authentication information
==========================

If you install this application on your own, you have to place all the
authentication information in a file called conf/auth.conf that will be loaded
on the application startup, but not stored in Github.