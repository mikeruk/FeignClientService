package dating.FeignClientService;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Getter
@Data
public class RegistrationData2 {

    private String username;
    private String password;

    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    Date registrationDate;

    public RegistrationData2() {
    }

    public RegistrationData2(String username, String password, Date registrationDate) {
        this.username = username;
        this.password = password;
        this.registrationDate = registrationDate;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }
}
