package ir.sepahan.app.loyalty;

/** منابع رویداد کسب امتیاز -- طبق ADR-0014؛ هر کدام حداکثر یک {@link PointsEarningRule} فعال دارد. */
public enum EarningSourceType {
    shop_order, ticket_purchase, football_ticket_purchase
}
