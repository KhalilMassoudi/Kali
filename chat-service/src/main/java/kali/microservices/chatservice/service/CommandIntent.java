package kali.microservices.chatservice.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandIntent {

    /** Action détectée: create_vps | list_vps | delete_vps |
     *  start_vps | stop_vps | reboot_vps | list_volumes |
     *  open_ticket | list_tickets |
     *  show_billing |
     *  clarify | none */
    private String action;

    /** Paramètres extraits par OpenAI (os, ram, cpu, name...) */
    private Map<String, Object> params = new HashMap<>();

    /** Message naturel à afficher à l'utilisateur */
    private String responseMessage;

    public boolean isActionable() {
        return action != null
                && !action.equals("none")
                && !action.equals("clarify");
    }
}
