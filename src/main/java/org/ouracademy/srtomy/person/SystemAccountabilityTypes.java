package org.ouracademy.srtomy.person;

import java.util.List;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SystemAccountabilityTypes {

    // Person and Org (like location, group or ministry)
    public Long VISITOR = 1L;
    public Long MEMBERSHIP_IN_PROGRESS = 2L;
    public Long MEMBER = 3L;

    // Party and Org
    public Long IS_PART_OF = 4L;
    // Person to group/ministry
    public Long LEADER = 5L;
    // Person to location
    public Long PASTOR = 6L;
    public Long WORSHIP_LEADER = 7L;
    public Long USHER = 8L;

    public List<Long> MEMBER_ACCOUNTABILITIES = List.of(VISITOR, MEMBERSHIP_IN_PROGRESS, MEMBER);
}
