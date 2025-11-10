package com.example.restrosuite.controller;

import com.example.restrosuite.entity.Bill;
import com.example.restrosuite.entity.Ingredient;
import com.example.restrosuite.entity.Order;
import com.example.restrosuite.entity.Payroll;
import com.example.restrosuite.entity.PurchaseOrder;
import com.example.restrosuite.repository.BillRepository;
import com.example.restrosuite.repository.IngredientRepository;
import com.example.restrosuite.repository.OrderRepository;
import com.example.restrosuite.repository.PayrollRepository;
import com.example.restrosuite.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @GetMapping("/sales-summary")
    public Map<String, Object> getSalesSummary(@RequestParam(required = false) String period) {
        LocalDateTime startDate = null;
        LocalDateTime endDate = LocalDateTime.now();
        
        if (period != null) {
            LocalDate today = LocalDate.now();
            switch (period.toLowerCase()) {
                case "today":
                    startDate = today.atStartOfDay();
                    break;
                case "week":
                    startDate = today.minusDays(7).atStartOfDay();
                    break;
                case "month":
                    startDate = today.minusMonths(1).atStartOfDay();
                    break;
                case "year":
                    startDate = today.minusYears(1).atStartOfDay();
                    break;
            }
        }
        
        List<Order> orders;
        if (startDate != null) {
            orders = orderRepository.findByCreatedAtBetween(startDate, endDate);
        } else {
            orders = orderRepository.findAll();
        }

        double totalSales = orders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        long totalOrders = orders.size();

        Map<String, Object> result = new HashMap<>();
        result.put("totalSales", totalSales);
        result.put("totalOrders", totalOrders);
        result.put("avgOrderValue", totalSales / (totalOrders == 0 ? 1 : totalOrders));
        result.put("period", period != null ? period : "all");

        return result;
    }
    
    @GetMapping("/sales-trends")
    public Map<String, Object> getSalesTrends() {
        LocalDate today = LocalDate.now();
        
        // Today's stats
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = LocalDateTime.now();
        List<Order> todayOrders = orderRepository.findByCreatedAtBetween(todayStart, todayEnd);
        double todaySales = todayOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        long todayCount = todayOrders.size();
        
        // Yesterday's stats (for comparison)
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = todayStart.minusSeconds(1);
        List<Order> yesterdayOrders = orderRepository.findByCreatedAtBetween(yesterdayStart, yesterdayEnd);
        double yesterdaySales = yesterdayOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        long yesterdayCount = yesterdayOrders.size();
        
        // This week's stats
        LocalDateTime weekStart = today.minusDays(7).atStartOfDay();
        List<Order> weekOrders = orderRepository.findByCreatedAtBetween(weekStart, todayEnd);
        double weekSales = weekOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        long weekCount = weekOrders.size();
        
        // Last week's stats (for comparison)
        LocalDateTime lastWeekStart = today.minusDays(14).atStartOfDay();
        LocalDateTime lastWeekEnd = weekStart.minusSeconds(1);
        List<Order> lastWeekOrders = orderRepository.findByCreatedAtBetween(lastWeekStart, lastWeekEnd);
        double lastWeekSales = lastWeekOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        long lastWeekCount = lastWeekOrders.size();
        
        // This month's stats
        LocalDateTime monthStart = today.minusMonths(1).atStartOfDay();
        List<Order> monthOrders = orderRepository.findByCreatedAtBetween(monthStart, todayEnd);
        double monthSales = monthOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        long monthCount = monthOrders.size();
        
        // Last month's stats (for comparison)
        LocalDateTime lastMonthStart = today.minusMonths(2).atStartOfDay();
        LocalDateTime lastMonthEnd = monthStart.minusSeconds(1);
        List<Order> lastMonthOrders = orderRepository.findByCreatedAtBetween(lastMonthStart, lastMonthEnd);
        double lastMonthSales = lastMonthOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        long lastMonthCount = lastMonthOrders.size();
        
        Map<String, Object> result = new HashMap<>();
        
        // Today with trend
        Map<String, Object> todayData = new HashMap<>();
        todayData.put("sales", todaySales);
        todayData.put("orders", todayCount);
        todayData.put("salesChange", yesterdaySales > 0 ? ((todaySales - yesterdaySales) / yesterdaySales) * 100 : 0);
        todayData.put("ordersChange", yesterdayCount > 0 ? ((todayCount - yesterdayCount) / (double) yesterdayCount) * 100 : 0);
        result.put("today", todayData);
        
        // Week with trend
        Map<String, Object> weekData = new HashMap<>();
        weekData.put("sales", weekSales);
        weekData.put("orders", weekCount);
        weekData.put("salesChange", lastWeekSales > 0 ? ((weekSales - lastWeekSales) / lastWeekSales) * 100 : 0);
        weekData.put("ordersChange", lastWeekCount > 0 ? ((weekCount - lastWeekCount) / (double) lastWeekCount) * 100 : 0);
        result.put("week", weekData);
        
        // Month with trend
        Map<String, Object> monthData = new HashMap<>();
        monthData.put("sales", monthSales);
        monthData.put("orders", monthCount);
        monthData.put("salesChange", lastMonthSales > 0 ? ((monthSales - lastMonthSales) / lastMonthSales) * 100 : 0);
        monthData.put("ordersChange", lastMonthCount > 0 ? ((monthCount - lastMonthCount) / (double) lastMonthCount) * 100 : 0);
        result.put("month", monthData);
        
        return result;
    }
    
    @GetMapping("/recent-orders")
    public List<Order> getRecentOrders(@RequestParam(defaultValue = "10") int limit) {
        return orderRepository.findAllOrderByCreatedAtDesc().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @GetMapping("/low-stock")
    public List<Ingredient> getLowStockItems() {
        return ingredientRepository.findAll().stream()
                .filter(i -> i.getQuantity() <= i.getThreshold())
                .toList();
    }

    @GetMapping("/profit-loss")
    public Map<String, Object> getProfitAndLoss(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = startDate != null ? LocalDate.parse(startDate).atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = endDate != null ? LocalDate.parse(endDate).atTime(23, 59, 59) : LocalDateTime.now();

        // Revenue (from Bills)
        List<Bill> bills = billRepository.findAll().stream()
                .filter(bill -> bill.getGeneratedAt() != null &&
                        !bill.getGeneratedAt().isBefore(start) &&
                        !bill.getGeneratedAt().isAfter(end))
                .toList();
        double totalRevenue = bills.stream()
                .mapToDouble(Bill::getGrandTotal)
                .sum();

        // Expenses
        // 1. Purchase Orders
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getDate() != null &&
                        !po.getDate().isBefore(start) &&
                        !po.getDate().isAfter(end))
                .toList();
        double purchaseExpenses = purchaseOrders.stream()
                .mapToDouble(po -> (po.getCost() * po.getQuantity()))
                .sum();

        // 2. Payroll Expenses
        List<Payroll> payrolls = payrollRepository.findAll().stream()
                .filter(p -> p.getPaymentDate() != null &&
                        !p.getPaymentDate().isBefore(start.toLocalDate()) &&
                        !p.getPaymentDate().isAfter(end.toLocalDate()) &&
                        "PAID".equals(p.getStatus()))
                .toList();
        double payrollExpenses = payrolls.stream()
                .mapToDouble(p -> p.getNetSalary() != null ? p.getNetSalary() : 0.0)
                .sum();

        double totalExpenses = purchaseExpenses + payrollExpenses;
        double netProfit = totalRevenue - totalExpenses;
        double profitMargin = totalRevenue > 0 ? (netProfit / totalRevenue) * 100 : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", start.toLocalDate().toString());
        result.put("endDate", end.toLocalDate().toString());
        result.put("revenue", totalRevenue);
        result.put("expenses", totalExpenses);
        result.put("purchaseExpenses", purchaseExpenses);
        result.put("payrollExpenses", payrollExpenses);
        result.put("netProfit", netProfit);
        result.put("profitMargin", profitMargin);
        result.put("totalBills", bills.size());
        result.put("totalPurchaseOrders", purchaseOrders.size());
        result.put("totalPayrolls", payrolls.size());

        return result;
    }

    @GetMapping("/cash-flow")
    public Map<String, Object> getCashFlow(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = startDate != null ? LocalDate.parse(startDate).atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = endDate != null ? LocalDate.parse(endDate).atTime(23, 59, 59) : LocalDateTime.now();

        // Cash Inflows (from Bills - assuming all bills are paid)
        List<Bill> bills = billRepository.findAll().stream()
                .filter(bill -> bill.getGeneratedAt() != null &&
                        !bill.getGeneratedAt().isBefore(start) &&
                        !bill.getGeneratedAt().isAfter(end))
                .toList();
        double cashInflows = bills.stream()
                .mapToDouble(Bill::getGrandTotal)
                .sum();

        // Cash Outflows
        // 1. Purchase Orders (paid)
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getDate() != null &&
                        !po.getDate().isBefore(start) &&
                        !po.getDate().isAfter(end))
                .toList();
        double purchaseOutflows = purchaseOrders.stream()
                .mapToDouble(po -> (po.getCost() * po.getQuantity()))
                .sum();

        // 2. Payroll Payments
        List<Payroll> payrolls = payrollRepository.findAll().stream()
                .filter(p -> p.getPaymentDate() != null &&
                        !p.getPaymentDate().isBefore(start.toLocalDate()) &&
                        !p.getPaymentDate().isAfter(end.toLocalDate()) &&
                        "PAID".equals(p.getStatus()))
                .toList();
        double payrollOutflows = payrolls.stream()
                .mapToDouble(p -> p.getNetSalary() != null ? p.getNetSalary() : 0.0)
                .sum();

        double totalOutflows = purchaseOutflows + payrollOutflows;
        double netCashFlow = cashInflows - totalOutflows;

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", start.toLocalDate().toString());
        result.put("endDate", end.toLocalDate().toString());
        result.put("cashInflows", cashInflows);
        result.put("cashOutflows", totalOutflows);
        result.put("purchaseOutflows", purchaseOutflows);
        result.put("payrollOutflows", payrollOutflows);
        result.put("netCashFlow", netCashFlow);
        result.put("totalBills", bills.size());
        result.put("totalPurchaseOrders", purchaseOrders.size());
        result.put("totalPayrolls", payrolls.size());

        return result;
    }

}

