package edu.market;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivityService {
    private final ActivityRepository activities;
    private final AccountRepository accounts;

    public ActivityService(ActivityRepository activities, AccountRepository accounts) {
        this.activities = activities;
        this.accounts = accounts;
    }

    public void record(String actor, String objectType, Long objectId, String operation, String outcome) {
        // Persist only identifiers and transition names. Never store addresses, credentials or payment input.
        activities.save(new Activity(actor, objectType, objectId, operation, outcome));
    }

    @Transactional(readOnly = true)
    public Page<Activity> search(String username, String actor, String objectType, Long objectId,
                                 Instant from, Instant until, int page) {
        Account account = accounts.findByUsername(username).orElseThrow(() -> new BusinessException("请先登录"));
        BusinessException.require(account.getRole().equals("ADMIN"), "只有管理员可查询操作记录");
        BusinessException.require(page >= 0 && page <= 100000, "页码无效");
        BusinessException.require(from == null || until == null || !from.isAfter(until), "时间区间倒置");
        Specification<Activity> spec = (root, query, cb) -> cb.conjunction();
        if (actor != null && !actor.isBlank()) {
            spec = spec.and((r, q, c) -> c.equal(r.get("actor"), actor));
        }
        if (objectType != null && !objectType.isBlank()) {
            spec = spec.and((r, q, c) -> c.equal(r.get("objectType"), objectType));
        }
        if (objectId != null) {
            spec = spec.and((r, q, c) -> c.equal(r.get("objectId"), objectId));
        }
        if (from != null) {
            spec = spec.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("occurredAt"), from));
        }
        if (until != null) {
            spec = spec.and((r, q, c) -> c.lessThanOrEqualTo(r.get("occurredAt"), until));
        }
        return activities.findAll(spec, PageRequest.of(page, 30, Sort.by("id").descending()));
    }
}
