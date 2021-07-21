package exceptions;

public class WrongPasswordException extends Exception {

	private static final long serialVersionUID = 6400742470407962441L;

	private String str;

	public WrongPasswordException() {
		this.str = "Wrong Password";
	}

	@Override
	public String toString() {
		return str;
	}
}
