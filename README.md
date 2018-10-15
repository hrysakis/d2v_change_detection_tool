
# SHORT DESCRIPTION

D2V is a flexible and powerful tool for defining, representing and detecting custom changes between datasets versions. We treat changes as first-class citizens, in a way that supports different modes of navigation on the deltas between any pair of versions, as well as the visualization of the evolution history of a given dataset.
D2V is a tool for assisting users in managing, querying and visualizing the evolution history of dynamic RDF datasets. Specifically, our goal is to provide an adequate user interface, allowing the management (creation, edit, delete) of complex changes, as well as the visualization of the contents of the ontology of changes, to enable the visual analysis of dataset dynamics. The tool can support users with a shared or dedicated personal space. To help first-time users, personal accounts can come as totally blank (no changes defined) or with some predefined changes for guidance. 

# USED TECHNOLOGIES
D2V is a cross-platform application implemented on top of open-source infrastructures and technologies. These technologies, such as Resource DescriptionFramework (RDF), facilitate and ensure all the appropriate management operations on semantic web data, during a change detection process.

It was implemented in Java programming language, the user interface was developed using
HTML/CSS technologies in combination with JQuery and Java Servlets which were implemented for the communication with the RDF store. For the visualization of results, we used the Google Charts Library which is an open library that aims to create customizable charts which are rendered using HTML5/SVG to provide cross-browser compatibility. Google Charts Library also comes with an API that supports many types of charts and operations upon them.

D2V stores all change definitions and the detected changes in an ontology of changes that is hosted in a Virtuoso Triplestore. In fact, the change definitions are stored in the schema level whereas the detected changes are stored in the instance level of the ontology. All queries are encoded in SPARQL; a query language which has become a standard and one of the key technologies for the semantic web.


# INSTALLATION (V 6.3)

a.Install and setup Virtuoso Store. 
Edit the properties file accordingly by placing values for: Repository_IP, Repository_Username, Repository_Password and Repository_Port.

b. Create the datasets graph.

You have to create the internal named graph http://datasets  in which the D2V stores the meta-data for both datasets and change detections. Specifically, this named graph contains information about all assigned dataset URIs and their corresponding versions. This can be performed through SPARQL for example CREATE GRAPH <http://datasets>; You can use the Interactive SQL Tool from Virtuoso conductor.


c. Upload your first dataset [guest user].

After login to the system as a guest, click on "options" button and then choose the action "Add a new dataset". Enter the dataset label e.g. EFO and the version label (used to be something like V1, V2 etc). Afterwards, click on browse to locate your dataset file version and then click on "upload". If everything so far is correct a message will be appeared after the data ingestion: "Your dataset version has been uploaded! Finally click on the "Add" button to complete this task. You have just added the dataset version EFO V1. You have to add one more version for this dataset. So you have to click on the "Options" button and then choose action "Add Version to Dataset". Then you select the dataset below (EFO) and the upload the new version (let's say V2) repeating the previous steps. Now D2V is completely functional as it requires minimum two dataset versions in order to a change detection makes sense. You are absolutely ready to check all the functions of the D2V system...

d. Optional. Use the multiple-users functionality

From the first screen menu you have three more options to login as a new user that keeps its own set of changes and datasets. In fact D2V can support multiple users; each one could have it's own datasetURI i.e http://default-dataset/user1, http://default-dataset/user2 etc.


e. Administration and configuration files.

You can edit config_generic.properties (for generic RDF model usage) located in the config folder. This file contains Virtuoso credentials information plus the following properties:
- i)'Simple_Changes_Folder' denotes the folder which contains the SPARQL update queries for the detection of Simple Changes. This is an essential update at the properties file that requires from user to set the actual full file path of this folder, which is by default inside the webapp folder.
- ii)'Simple_Changes' denotes the list of considered Simple Changes to be detected
- iii) 'Dataset_URIs' which denotes namedgraphs associated with the corresponding dataset versions within virtuoso, used optionally in a new user assignment
- iv) 'Dataset_Files_Folder' 'Dataset_Default_Schema' contain options for enabling the management of datasets. If you like to disable this feature just put # at these two parameters of the config_generic.properties file.


# DEMO VIDEO

You can found a demo video showing the basic functionalities. You can view it by open the video.html file which is located in src/main/webapp/ folder.

# CONTACT

For any information about the D2V system please contact Ioannis Chrysakis via e-mail: hrysakis@ics.forth.gr


# INFO
More details about this work can be found at the following publications: 

-Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis, Giorgos Flouris. "D2V: A Tool for Defining, Detecting and Visualizing Changes on the Data Web". In Proceedings of the 14th International Semantic Web Conference, Posters and Demonstrations Track (ISWC-15), 2015. 

-Kostas Stefanidis, Giorgos Flouris, Ioannis Chrysakis, Yannis Roussakis. (2016). D2V – Understanding the Dynamics of Evolving Data: A Case Study in the Life Sciences. ERCIM News 2016 (105), April 2016.

-Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis, Giorgos Flouris, Yannis Stavrakas. "A Flexible Framework for Understanding the Dynamics of Evolving RDF Datasets". FORTH-ICS, Technical Report 456, 2015.  

-Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis, Giorgos Flouris, Yannis Stavrakas. "A Flexible Framework for Understanding the Dynamics of Evolving RDF Datasets". In Proceedings of the 14th International Semantic Web Conference (ISWC-15), 2015. Best Student Research Paper Award.  

# COPYRIGHT

Copyright 2015-18, FORTH-ICS, All Rights Reserved.
Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis and Giorgos Flouris.
Foundation for Research and Technology Hellas, Institute of Computer Science.
