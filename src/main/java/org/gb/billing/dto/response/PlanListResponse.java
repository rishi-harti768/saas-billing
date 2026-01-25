package org.gb.billing.dto.response;

import java.util.List;

/**
 * Response DTO for paginated list of billing plans.
 * 
 * Contains the list of plans and pagination metadata.
 */
public class PlanListResponse {

    private List<PlanResponse> plans;
    private int totalCount;
    private int pageNumber;
    private int pageSize;

    // Constructors
    public PlanListResponse() {
    }

    public PlanListResponse(List<PlanResponse> plans, int totalCount, int pageNumber, int pageSize) {
        this.plans = plans;
        this.totalCount = totalCount;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    // Getters and Setters
    public List<PlanResponse> getPlans() {
        return plans;
    }

    public void setPlans(List<PlanResponse> plans) {
        this.plans = plans;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    public boolean hasNext() {
        return pageNumber < getTotalPages() - 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }
}
