package com.visallo;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App
{
    private static final String RESULT_SEP = "======================";
    private static final String USAGE = "java SomeCoDataCreator <username> <password> <root folder>";

    public static void main(String[] args) {
        if (args.length != 3) doUsage(App.USAGE);
        App sccdq = new App();
        sccdq.setUser(args[0]);
        sccdq.setPassword(args[1]);
        sccdq.setFolderName(args[2]);
        sccdq.doExamples();
    }

    /**
     * Executes a series of CMIS Query Language queries and dumps the results.
     */
    public void doExamples() {
        String queryString;



        System.out.println(RESULT_SEP);
//        System.out.println("Finding content of type:" + SomeCoModel.TYPE_SC_DOC.toString());
        queryString = "select * from sc:doc";
        dumpQueryResults(getQueryResults(queryString));

//        System.out.println(RESULT_SEP);
//        System.out.println("Find content in the root folder with text like 'sample'");
//        queryString = "select * from cmis:document where contains('sample') and in_folder('" + getFolderId(getFolderName()) + "')";
//        dumpQueryResults(getQueryResults(queryString));
//
//        System.out.println(RESULT_SEP);
//        System.out.println("Find active content");
//        queryString = "select d.*, w.* from cmis:document as d join sc:webable as w on d.cmis:objectId = w.cmis:objectId where w.sc:isActive = True";
//        dumpQueryResults(getQueryResults(queryString));
//
//        System.out.println(RESULT_SEP);
//        System.out.println("Find active content with a product equal to 'SomePortal'");
//        // There is no way to do a join across two aspects and subqueries aren't supported so we
//        // are forced to execute two queries.
//        String queryString1 = "select d.cmis:objectId from cmis:document as d join sc:productRelated as p on d.cmis:objectId = p.cmis:objectId " +
//                "where p.sc:product = 'SomePortal'";
//        String queryString2 = "select d.cmis:objectId from cmis:document as d join sc:webable as w on d.cmis:objectId = w.cmis:objectId " +
//                "where w.sc:isActive = True";
//        dumpQueryResults(getSubQueryResults(queryString1, queryString2));
//
//        System.out.println(RESULT_SEP);
//        System.out.println("Find content of type sc:whitepaper published between 1/1/2006 and 6/1/2007");
//        queryString = "select d.cmis:objectId, w.sc:published from sc:whitepaper as d join sc:webable as w on d.cmis:objectId = w.cmis:objectId " +
//                "where w.sc:published > TIMESTAMP '2006-01-01T00:00:00.000-05:00' " +
//                "  and w.sc:published < TIMESTAMP '2007-06-02T00:00:00.000-05:00'";
//        dumpQueryResults(getQueryResults(queryString));
    }

    /**
     * Executes the query string provided and returns the results as a list of CmisObjects.
     * @param queryString
     * @return
     */
    public List<CmisObject> getQueryResults(String queryString) {
        Session session = getSession();
        List<CmisObject> objList = new ArrayList<CmisObject>();

        // execute query
        ItemIterable<QueryResult> results = session.query(queryString, false).getPage(5);
        for (QueryResult qResult : results) {
            String objectId = "";
            PropertyData<?> propData = qResult.getPropertyById("cmis:objectId"); // Atom Pub binding
            if (propData != null) {
                objectId = (String) propData.getFirstValue();
            } else {
                objectId = qResult.getPropertyValueByQueryName("d.cmis:objectId"); // Web Services binding
            }
            CmisObject obj = session.getObject(session.createObjectId(objectId));
            objList.add(obj);
        }
        return objList;
    }

    /**
     * Executes queryString1 to retrieve a set of objectIDs which are then used in an
     * IN predicate appended to the second query. Assumes the second query defines an
     * alias called "d", as in "select d.cmis:objectId from cmis:document as d"
     *
     * @param queryString1
     * @param queryString2
     * @return
     */
    public List<CmisObject> getSubQueryResults(String queryString1, String queryString2) {
        List<CmisObject> objList = new ArrayList<CmisObject>();

        // execute first query
        ItemIterable<QueryResult> results = getSession().query(queryString1, false);
        List<String> objIdList = new ArrayList<String>();
        for (QueryResult qResult : results) {
            String objectId = "";
            PropertyData<?> propData = qResult.getPropertyById("cmis:objectId"); // Atom Pub Binding
            if (propData != null) {
                objectId = (String) propData.getFirstValue();
            } else {
                objectId = qResult.getPropertyValueByQueryName("d.cmis:objectId"); // Web Services Binding
            }
            objIdList.add(objectId);
        }

        if (objIdList.isEmpty()) {
            return objList;
        }

        String queryString = queryString2 + " AND d.cmis:objectId IN " + getPredicate(objIdList);
        return getQueryResults(queryString);
    }

    /**
     * Returns a comma-separated list of object IDs provided as a List.
     * @param objectIdList
     * @return String
     */
    public String getPredicate(List<String> objectIdList) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < objectIdList.size(); i++) {
            sb.append("'");
            sb.append(objectIdList.get(i));
            sb.append("'");
            if (i >= objectIdList.size() - 1) { break; }
            sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Dumps the object ID, name, and creation date for each CmisObject in the List provided.
     * @param results
     */
    public void dumpQueryResults(List<CmisObject> results) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        int iCount = 1;
        for (CmisObject result : results) {
            System.out.println("----------------------\r\nResult " + iCount + ":");
            System.out.println("id:" + result.getId());
            System.out.println("name:" + result.getName());
            System.out.println("created:" + dateFormat.format(result.getCreationDate().getTime()));
            //System.out.println("url:" + ???); // No easy way to get this, unfortunately
            iCount ++;
        }
    }



    private String user;
    private String password;
    private String folderName;

    public static void doUsage(String message) {
        System.out.println(message);
        System.exit(0);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    //private String serviceUrl = "http://localhost:8080/alfresco/cmisatom"; // Uncomment for Atom Pub binding
    //private String serviceUrl = "http://localhost:8080/alfresco/cmis"; // Uncomment for Web Services binding
    private String serviceUrl = "http://localhost:8082/alfresco/api/-default-/public/cmis/versions/1.0/atom"; // Uncomment for Atom Pub binding
    //private String serviceUrl = "http://localhost:8080/alfresco/api/-default-/public/cmis/versions/cmisws"; // Uncomment for Web Services binding
    //private String serviceUrl = "http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom"; // Uncomment for Atom Pub binding
    //private String serviceUrl = "http://jpotts.alfresco-laptop.com:8081/chemistry/browser";
    private Session session = null;

    private String contentType;
    private String contentName;

    public Session getSession() {
        if (session == null) {
            // default factory implementation
            SessionFactory factory = SessionFactoryImpl.newInstance();
            Map<String, String> parameter = new HashMap<String, String>();

            // user credentials
            parameter.put(SessionParameter.USER, getUser());
            parameter.put(SessionParameter.PASSWORD, getPassword());

            // connection settings
            parameter.put(SessionParameter.ATOMPUB_URL, getServiceUrl()); // Uncomment for Atom Pub binding
            parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value()); // Uncomment for Atom Pub binding

            //parameter.put(SessionParameter.BROWSER_URL, getServiceUrl()); // Uncomment for Browser binding
            //parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value()); // Uncomment for Browser binding

            // Uncomment for Web Services binding
			/*
			parameter.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value()); // Uncomment for Web Services binding

			parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, getServiceUrl() + "/ACLService");
			parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, getServiceUrl() + "/DiscoveryService");
			parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, getServiceUrl() + "/MultiFilingService");
			parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, getServiceUrl() + "/NavigationService");
			parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, getServiceUrl() + "/ObjectService");
			parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, getServiceUrl() + "/PolicyService");
			parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, getServiceUrl() + "/RelationshipService");
			parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, getServiceUrl() + "/RepositoryService");
			parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, getServiceUrl() + "/VersioningService");
			*/

            // Set the alfresco object factory
            // Used when using the CMIS extension for Alfresco for working with aspects
            parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

            List<Repository> repositories = factory.getRepositories(parameter);

            this.session = repositories.get(0).createSession();
        }
        return this.session;
    }

    /**
     * Gets the object ID for a folder of a specified name which is assumed to be unique across the
     * entire repository.
     *
     * @return String
     */
    public String getFolderId(String folderName) {
        String objectId = null;
        String queryString = "select cmis:objectId from cmis:folder where cmis:name = '" + folderName + "'";
        ItemIterable<QueryResult> results = getSession().query(queryString, false);
        for (QueryResult qResult : results) {
            objectId = qResult.getPropertyValueByQueryName("cmis:objectId");
        }
        return objectId;
    }

    public Document createTestDoc(String docName, String contentType) {
        Session session = getSession();

        // Grab a reference to the folder where we want to create content
        Folder folder = (Folder) session.getObjectByPath("/" + getFolderName());

        String timeStamp = new Long(System.currentTimeMillis()).toString();
        String filename = docName + " (" + timeStamp + ")";

        // Create a Map of objects with the props we want to set
        Map <String, Object> properties = new HashMap<String, Object>();
        // Following sets the content type and adds the webable and productRelated aspects
        // This works because we are using the OpenCMIS extension for Alfresco
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:sc:whitepaper,P:sc:webable,P:sc:productRelated");
        properties.put(PropertyIds.NAME, filename);
		/*
		properties.put("sc:isActive", true);
		GregorianCalendar publishDate = new GregorianCalendar(2007,4,1,5,0);
		properties.put("sc:published", publishDate);
		*/
        String docText = "This is a sample " + contentType + " document called " + docName;
        byte[] content = docText.getBytes();
        ByteArrayInputStream stream = new ByteArrayInputStream(content);
        ContentStream contentStream = session.getObjectFactory().createContentStream(filename, Long.valueOf(content.length), "text/plain", stream);

        Document doc = folder.createDocument(
                properties,
                contentStream,
                VersioningState.MAJOR);
        System.out.println("Created: " + doc.getId());
        System.out.println("Content Length: " + doc.getContentStreamLength());

        return doc;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getContentName() {
        return this.contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
