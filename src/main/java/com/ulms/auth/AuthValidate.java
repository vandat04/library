package com.ulms.auth;

public class AuthValidate {

    boolean validateFullName(String fullName) {
        //Nếu tên trống thì out
        if (fullName == null) {
            return false;
        }
        //Kiểm tra xem có chưa các kí đặc biệt hay không
        return fullName.matches("^[a-zA-ZÀ-ỹ\\s'-]{2,100}$");
    }

    boolean validatePhoneNumber(String phone){
        if (phone == null){
            return false;
        }
        String[] spaceChar = phone.trim().split(" ");
        if(spaceChar.length > 1){
            return false;
        }
        return phone.trim().matches("^0[0-9]{9}$");
    }

    boolean validateEmail(String email){
        if (email == null){
            return false;
        }
        String[] spaceChar = email.trim().split(" ");
        if(spaceChar.length > 1){
            return false;
        }
        return email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    }

    boolean validatePassword(String password){
        if (password == null){
            return false;
        }
        String[] spaceChar = password.trim().split(" ");
        if(spaceChar.length > 1){
            return false;
        }
        //password >8 kí tự, có hoa, thường, số, kí tự đặc biệt
        return password.trim().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }

}
