
# SHORT DESCRIPTION

D2V is a flexible and powerful tool for defining, representing and detecting custom changes between datasets versions. We treat changes as first-class citizens, in a way that supports different modes of navigation on the deltas between any pair of versions, as well as the visualization of the evolution history of a given dataset.
D2V is a tool for assisting users in managing, querying and visualizing the evolution history of dynamic RDF datasets. Specifically, our goal is to provide an adequate user interface, allowing the management (creation, edit, delete) of complex changes, as well as the visualization of the contents of the ontology of changes, to enable the visual analysis of dataset dynamics. The tool can support users with a shared or dedicated personal space. To help first-time users, personal accounts can come as totally blank (no changes defined) or with some predefined changes for guidance. 

# USED TECHNOLOGIES
D2V is a cross-platform application implemented on top of open-sourceinfrastructures and technologies. These technologies, such as Resource DescriptionFramework (RDF), facilitate and ensure all the appropriate management operations on semantic web data, during a change detection process.

It was implemented in Java programming language, the user interface was developed using
HTML/CSS technologies in combination with JQuery and Java Servlets which were implemented for the communication with the RDF store. For the visualization of results, we used the Google Charts Library which is an open library that aims to create customizable charts which are rendered using HTML5/SVG to provide cross-browser compatibility. Google Charts Library also comes with an API that supports many types of charts and operations upon them.

D2V stores all change definitions and the detected changes in an ontology of changes that is hosted in a Virtuoso Triplestore. In fact, the change definitions are stored in the schema level whereas the detected changes are stored in the instance level of the ontology. All queries are encoded in SPARQL; a query language which has become a standard and one of the key technologies for the semantic web.


# INSTALLATION:

a. Install and setup Virtuoso Store (If you want to use the installed one which has been already configured,skip this step).

In general the management of datasets within Virtuoso includes the creation of one or more dataset URIs, the loading of default RDF schema(s) and the uploading  of at least two versions assigned to each datasetURI. Thus first you have to create the internal named graph http://datasets to store the meta-data for both the datasets and the change detections. This named graph contains information about all assigned dataset URIs and their corresponding versions. Secondly, you should create the appropriate dataset URI and import a default schema assinged to it and located in datasetURI/changes/schema. Finally, you need to create one named graph per version and upload the version contents to it.
For instance, consider the EFO datasets. It holds that the dataset URI is http://www.ebi.ac.uk/efo/ and we can find the triples:
-http://www.ebi.ac.uk/efo/ rdfs:member http://www.diachron-fp7.eu/resource/recordset/efo/2.34     -http://www.ebi.ac.uk/efo/ rdfs:member http://www.diachron-fp7.eu/resource/recordset/efo/2.35 

Note that the initial datasetURI should have the form datasetURI/guest to formulate that it is assinged to the default guest user. If you don't want to use the guest user, you can enable dataset options from config_generic.properties (see below). D2V can support multiple users; each own could have it's own datasetURI i.e http://www.ebi.ac.uk/efo/user1, http://www.ebi.ac.uk/efo/user2 etc.

b. Upload the corresponding war file which is located inside target folder in a webserver (Tomcat, Glassfish, etc)

c. Edit config_generic.properties (for generic RDF model usage) or config_EFO.properties file (for DIACHRON model usage) from Config folder. This file contains Virtuoso credentials information plus the following properties:
- i)'Simple_Changes_Folder' denotes the folder which contains the SPARQL update queries for the detection of Simple Changes. This is an essestial update at the properties file that requires from user to set the actual full file path of this folder, which is by default inside the webapp folder.
- ii)'Simple_Changes' denotes the list of considered Simple Changes to be detected
- iii) 'Dataset_URIs' which denotes namedgraphs associated with the corresponding dataset versions within virtuoso, used optionally in a new user assignment
- iv) 'Dataset_Files_Folder' 'Dataset_Default_Schema' contain options for enabling the management of datasets for admins. To enable them remove#.


# DEMO VIDEO

You can found a demo video showing the basic functionalities. You can view it by open the video.html file which is located in src/main/webapp/ folder.

# COPYRIGHT
Copyright 2015-16, FORTH-ICS, All Rights Reserved.
Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis and Giorgos Flouris.
Foundation for Research and Technology Hellas, Institute of Computer Science.

#INFO
For any information about the D2V system please contact Ioannis Chrysakis via e-mail: hrysakis@ics.forth.gr
More details can be found at the following publications: 

Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis, Giorgos Flouris, Yannis Stavrakas. "A Flexible Framework for Understanding the Dynamics of Evolving RDF Datasets". In Proceedings of the 14th International Semantic Web Conference (ISWC-15), 2015. Best Student Research Paper Award.  

Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis, Giorgos Flouris. "D2V: A Tool for Defining, Detecting and Visualizing Changes on the Data Web". In Proceedings of the 14th International Semantic Web Conference, Posters and Demonstrations Track (ISWC-15), 2015. 
