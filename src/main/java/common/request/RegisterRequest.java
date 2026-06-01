package common.request;

import common.enums.Role;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest implements Serializable {
  private static final long serialVersionUID = 1L;
  private String username;
  private String password;
  private String firstname;
  private String lastname;
  private Role role;
}
