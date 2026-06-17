package ru.copperside.paylimits.projection.reservation.adapter.out.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.copperside.paylimits.projection.reservation.application.port.out.ReservationQueryStore;
import ru.copperside.paylimits.projection.reservation.application.view.ReservationEventView;
import ru.copperside.paylimits.projection.reservation.application.view.ReservationStateView;
import ru.copperside.paylimits.projection.reservation.application.view.ReservationSummaryRow;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcReservationQueryStore implements ReservationQueryStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReservationQueryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<ReservationStateView> STATE = (rs, n) -> new ReservationStateView(
            rs.getString("reservation_id"), rs.getString("operation_id"), rs.getString("state"),
            rs.getString("merchant_id"), rs.getString("operation_type"), rs.getString("direction"),
            rs.getBigDecimal("amount").toPlainString(), rs.getString("currency"),
            instant(rs.getTimestamp("held_at")), instant(rs.getTimestamp("last_occurred_at")),
            instant(rs.getTimestamp("stale_after")));

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    @Override
    public Optional<ReservationStateView> findByReservationId(UUID reservationId) {
        return jdbcTemplate.query("select * from reservation_state where reservation_id = ?", STATE, reservationId)
                .stream().findFirst();
    }

    @Override
    public Optional<ReservationStateView> findByOperationId(String operationId) {
        return jdbcTemplate.query("select * from reservation_state where operation_id = ?", STATE, operationId)
                .stream().findFirst();
    }

    @Override
    public List<ReservationStateView> list(String merchantId, String state, Instant from, Instant to, int page, int size) {
        StringBuilder sql = new StringBuilder("select * from reservation_state where 1 = 1");
        List<Object> args = new ArrayList<>();
        if (merchantId != null) { sql.append(" and merchant_id = ?"); args.add(merchantId); }
        if (state != null) { sql.append(" and state = ?"); args.add(state); }
        if (from != null) { sql.append(" and last_occurred_at >= ?"); args.add(Timestamp.from(from)); }
        if (to != null) { sql.append(" and last_occurred_at < ?"); args.add(Timestamp.from(to)); }
        sql.append(" order by last_occurred_at desc limit ? offset ?");
        args.add(size);
        args.add((long) page * size);
        return jdbcTemplate.query(sql.toString(), STATE, args.toArray());
    }

    @Override
    public List<ReservationEventView> events(UUID reservationId) {
        return jdbcTemplate.query("""
                select event_id, event_type, state, occurred_at, amount, currency
                from reservation_event where reservation_id = ? order by occurred_at asc
                """,
                (rs, n) -> new ReservationEventView(
                        rs.getString("event_id"), rs.getString("event_type"), rs.getString("state"),
                        rs.getTimestamp("occurred_at").toInstant(), rs.getBigDecimal("amount").toPlainString(),
                        rs.getString("currency")),
                reservationId);
    }

    @Override
    public List<ReservationSummaryRow> summary(String merchantId, Instant from, Instant to, String groupBy) {
        boolean byDay = "day".equalsIgnoreCase(groupBy);
        String groupExpr = byDay ? "to_char(date_trunc('day', last_occurred_at), 'YYYY-MM-DD')" : "merchant_id";
        StringBuilder sql = new StringBuilder("select " + groupExpr + " as group_key, currency,"
                + " count(*) as confirmed_count, coalesce(sum(amount), 0) as confirmed_amount"
                + " from reservation_state where state = 'CONFIRMED'");
        List<Object> args = new ArrayList<>();
        if (merchantId != null) { sql.append(" and merchant_id = ?"); args.add(merchantId); }
        if (from != null) { sql.append(" and last_occurred_at >= ?"); args.add(Timestamp.from(from)); }
        if (to != null) { sql.append(" and last_occurred_at < ?"); args.add(Timestamp.from(to)); }
        sql.append(" group by group_key, currency order by group_key");
        return jdbcTemplate.query(sql.toString(),
                (rs, n) -> new ReservationSummaryRow(
                        rs.getString("group_key"), rs.getLong("confirmed_count"),
                        rs.getBigDecimal("confirmed_amount").toPlainString(), rs.getString("currency")),
                args.toArray());
    }
}
