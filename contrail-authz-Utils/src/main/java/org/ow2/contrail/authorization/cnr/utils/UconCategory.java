package org.ow2.contrail.authorization.cnr.utils;

public enum UconCategory {
    SUBJECT, RESOURCE, ACTION, ENVIRONMENT;
    
    public static UconCategory getCategoryFromTag(String tag) {
	if(tag == null) {
		// KMcC;) 
		System.out.println("{[UconCategory] [KMcC;)] getCategoryFromTag(): NULL POINTER EXCEPTION!");
	    throw new NullPointerException();
	}
	if(tag.equals(UconConstants.PIP_REQUEST_RESOURCE_Tag))
	    return UconCategory.RESOURCE;
	if(tag.equals(UconConstants.PIP_REQUEST_SUBJECT_Tag))
	    return UconCategory.SUBJECT;
	if(tag.equals(UconConstants.PIP_REQUEST_ACTION_Tag))
	    return UconCategory.ACTION;
	if(tag.equals(UconConstants.PIP_REQUEST_ENVIRONMENT_Tag))
	    return UconCategory.ENVIRONMENT;
	
	throw new IllegalArgumentException("invalid tag: "+tag);
    }
    
    public String getTagFromCategory() {
	switch(this) {
	case RESOURCE:
	    return UconConstants.PIP_REQUEST_RESOURCE_Tag;
	case SUBJECT:
	    return UconConstants.PIP_REQUEST_SUBJECT_Tag;
	case ACTION:
	    return UconConstants.PIP_REQUEST_ACTION_Tag;
	case ENVIRONMENT:
	    return UconConstants.PIP_REQUEST_ENVIRONMENT_Tag;
	default:
	    throw new RuntimeException();
	}
    }
}