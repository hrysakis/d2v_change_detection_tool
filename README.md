

# SHORT DESCRIPTION:
D2V is a flexible and powerful tool for defining, representing and detecting custom changes between datasets versions. We treat changes as first-class citizens, in a way that supports different modes of navigation on the deltas between any pair of versions, as well as the visualization of the evolution history of a given dataset.
D2V is a tool for assisting users in managing, querying and visualizing the evolution history of dynamic RDF datasets. Specifically, our goal is to provide an adequate user interface, allowing the management (creation, edit, delete) of complex changes, as well as the visualization of the contents of the ontology of changes, to enable the visual analysis of dataset dynamics. The tool can support users with a shared or dedicated personal space. To help first-time users, personal accounts can come as totally blank (no changes defined) or with some predefined changes for guidance. 

# USED TECHNOLOGIES:
D2V is a cross-platform application implemented on top of open-source
infrastructures and technologies. These technologies, such as Resource DescriptionFramework (RDF), facilitate and ensure all the appropriate management operations on semantic web data, during a change detection process.

It was implemented in Java programming language, the user interface was developed using
HTML/CSS technologies in combination with JQuery and Java Servlets which were implemented for the communication with the RDF store. For the visualization of results, we used the Google Charts Library which is an open library that aims to create customizable charts which are rendered using HTML5/SVG to provide cross-browser compatibility. Google Charts Library also comes with an API that supports many types of charts and operations upon them.

D2V stores all change definitions and the detected changes in an ontology of changes that is hosted in a Virtuoso Triplestore. In fact, the change definitions are stored in the schema level whereas the detected changes are stored in the instance level of the ontology. All queries are encoded in SPARQL; a query language which has become a standard and one of the key technologies for the semantic web.


# DEPLOYMENT /INSTALLATION:
