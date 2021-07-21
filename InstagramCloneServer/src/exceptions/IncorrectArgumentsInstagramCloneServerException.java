package exceptions;


public class IncorrectArgumentsInstagramCloneServerException extends Exception {
	
	private static final long serialVersionUID = 1L;
	private String str;
	
	public IncorrectArgumentsInstagramCloneServerException(String str) {
		this.str = str;
	}

	@Override
	public String toString() {
		return str;
	}
	

	
}
