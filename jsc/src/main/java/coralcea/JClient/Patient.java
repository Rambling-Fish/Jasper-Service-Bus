package coralcea.JClient;

 public class Patient {
     public enum Gender { MALE, FEMALE };
 
     public static class Name {
       private String _first, _last;
 
       public String getFirst() { return _first; }
       public String getLast() { return _last; }
 
       public void setFirst(String s) { _first = s; }
       public void setLast(String s) { _last = s; }
     }
 
     private String _gender;
     private Name _name;
     private boolean _isVerified;
     private String _heartRate;
 
     public Name getName() { return _name; }
     public boolean isVerified() { return _isVerified; }
     public String getGender() { return _gender; }
     public String getHeartRate() { return _heartRate; }
 
     public void setName(Name n) { _name = n; }
     public void setVerified(boolean b) { _isVerified = b; }
     public void setGender(String g) { _gender = g; }
     public void setHeartRate (String b) { _heartRate = b; }
		@Override
		public String toString() {
			return "Patient [_gender=" + _gender + ", _name=" + _name
					+ ", _isVerified=" + _isVerified + ", _heartRate=" + _heartRate + "]";
		}
     
     
 }
