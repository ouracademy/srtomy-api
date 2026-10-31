package org.ouracademy.ovejafiel.person.query.find_people_by_phone;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * The active people of the church {@code churchId} (any node below it) whose phone is {@code
 * phone}. Numbers are compared by their digits: they match when they are equal or when the one on
 * the record, of at least 8 digits, is the end of {@code phone} (the record may lack the country
 * code or carry a leading zero).
 */
public record FindPeopleByPhoneQuery(String phone, PartyId churchId) {}
