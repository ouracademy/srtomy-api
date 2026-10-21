package org.ouracademy.srtomy.complaint.repository;

import org.ouracademy.srtomy.complaint.domain.Complaint;
import org.ouracademy.srtomy.complaint.domain.ComplaintId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, ComplaintId> {}
