package org.ow2.contrail.authorization.cnr.pep.utils;


public enum PepState {
    VEP, INIT, PERMITTED, RUNNING, ENDED;

    public static final String ok = "ok";

    public String checkAction(PepAction action) {
	String response = ok;
	switch (action) {
	case TRY:
	    if (this != PepState.INIT) {
		if (this != PepState.VEP)
		    response = "You can do only one tryaccess for each pep_callout object";
		else
		    response = "A pep_callout object created with a session id can't do a tryacces";
	    }
	    break;
	case START:
	    switch (this) {
	    case INIT:
		response = "You must have the authorization before starting an action (call tryaccess first)";
	    case RUNNING:
		response = "You already start an action on this object";
	    case ENDED:
		response = "The action started is already ended or revoked by application";
	    case PERMITTED:
	    case VEP:
	    default:
		break;
	    }
	    break;
	case END:
	    switch (this) {
	    case INIT:
		response = "You must have the authorization before starting an action (call tryaccess first)";
	    case PERMITTED:
		response = "You must start an action before ending it (call startaccess first)";
	    case ENDED:
		response = "The action started is already ended or revoked by application";
	    case RUNNING:
	    case VEP:
	    default:
		break;
	    }
	    break;
	case MAP:
	    switch (this) {
	    case INIT:
		response = "You must have the authorization before map another id (call tryaccess first)";
	    case VEP:
		response = "The id is already mapped";
	    case ENDED:
		response = "The action is already ended or revoked by application";
	    case PERMITTED:
	    case RUNNING:
	    default:
		break;
	    }
	    break;
	default:
	    break;

	}
	return response;
    }

    public PepState changeState(PepAction action) {
	PepState state = this;
	switch (this) {
	case INIT:
	    switch (action) {
	    case TRY:
		state = PepState.PERMITTED;
		break;
	    default:
		break;
	    }
	    break;
	case PERMITTED:
	    switch (action) {
	    case MAP:
		state = PepState.VEP;
		break;
	    case START:
		state = PepState.RUNNING;
		break;
	    default:
		break;
	    }
	    break;
	case RUNNING:
	    switch (action) {
	    case END:
		state = PepState.ENDED;
		break;
	    case MAP:
		state = PepState.VEP;
		break;
	    default:
		break;
	    }
	    break;
	case VEP:
	    switch (action) {
	    case END:
		state = PepState.ENDED;
		break;
	    case START:
		state = PepState.RUNNING;
		break;
	    default:
		break;
	    }
	    break;
	case ENDED:
	    break;
	default:
	    break;

	}
	return state;
    }
}