package org.ouracademy.srtomy.organization;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RoleService {
    public void sync(Organization location, List<String> roles) {
        // location.roles.name not in roles => delete those not in // A - B
        // new roles not in location.roles => add // B - A

        // check if trying delete a role that is used then what should we do?
        // 1. remove role from that person

        // note delete can be hard or soft
        // if soft store => deleted = true or end_date = now()

        throw new UnsupportedOperationException("Unimplemented method 'syncRoles'");
    }

}
