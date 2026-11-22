package com.acxiom.crm.service;

import com.acxiom.crm.entity.Activity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.ActivityStatus;
import com.acxiom.crm.enums.ActivityType;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.viewmodel.ActivityForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ActivityService {

    Page<Activity> listActivities(User scopeUser, ActivityType type, ActivityStatus status,
                                  LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    Activity getActivity(Long id, User scopeUser);

    Activity createActivity(ActivityForm form, CrmUserDetailsService.CrmUserPrincipal principal);

    Activity updateActivity(Long id, ActivityForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    void deleteActivity(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    ActivityForm toForm(Activity activity);
}
