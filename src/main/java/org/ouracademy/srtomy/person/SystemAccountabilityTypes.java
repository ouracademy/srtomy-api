package org.ouracademy.srtomy.person;

public class SystemAccountabilityTypes {

    // Person and Org (like location, group or ministry)
    public static final Long VISITOR = 1L;
    public static final Long MEMBERSHIP_IN_PROGRESS = 2L;
    public static final Long MEMBER = 3L;

    // Party and Org
    public static final Long IS_PART_OF = 4L;
    // Person to group/ministry
    public static final Long LEADER = 5L;
    // Person to location
    public static final Long PASTOR = 6L;
    public static final Long WORSHIP_LEADER = 7L;
    public static final Long USHER = 8L;

    public static final Long[] MEMBER_ACCOUNTABILITIES = new Long[] { VISITOR, MEMBERSHIP_IN_PROGRESS, MEMBER };
}
