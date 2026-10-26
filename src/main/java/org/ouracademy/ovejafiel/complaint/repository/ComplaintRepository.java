package org.ouracademy.ovejafiel.complaint.repository;

import org.ouracademy.ovejafiel.complaint.domain.Complaint;
import org.ouracademy.ovejafiel.complaint.domain.ComplaintId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, ComplaintId> {}
