package org.ow2.contrail.authorization.cnr.old.pep;

//import java.util.logging.Level;
//import java.util.logging.Logger;

@Deprecated
public class Attribute  {

	//private static Logger logger = Logger.getLogger("cnr.contrail.pep.attribute");
	
	public final static int SUBJECT = 0; 
	public final static int RESOURCE = 1;
	public final static int ACTION = 2;
	public final static int ENVIRONMENT = 3;
	
    private String id = new String("");
    private String type = new String("");
    private String value = new String("");
    private String issuer = new String("");
    private int holder = 3;
    
    public Attribute( String id,  String type,  String value,  String issuer,  int holder){
        this.id = id;
        this.type = type;
        this.value = value;
        this.issuer = issuer;
        this.holder = holder;
    }
    
    public String getId(){
    	return id;
    }
    
    public String getType(){
    	return type;
    }

    public String getValue(){
    	return value;
    }

    public String getIssuer(){
    	return issuer;
    }
    
    public int getHolder(){
    	return holder;
    }    
    
    //should be depreceted
    public String createXACMLAttr() {
    		String attribute = new String("");
    		try {
    		attribute = 
    				"<xacml-context:Attribute " +
    						"AttributeId=\""+ id +"\" " +
    						"DataType=\""+ type +"\" " +
    						"Issuer=\""+ issuer +"\">" +
    						"<xacml-context:AttributeValue>"+ value +"</xacml-context:AttributeValue>" +
    				"</xacml-context:Attribute>";
   		 	}catch(Exception e){
   		 		 //DO SOMETHING
   		 		 //System.out.println("Attribute->createXACMLAttr:" + e.toString());
   		 		//logger.log(Level.WARNING, "Attribute->createXACMLAttr:", e);
   		 	}	 
    		
    		return attribute;
    }
}
