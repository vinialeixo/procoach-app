import { useState } from "react";
import {
  AthleteNotification,
  useAthleteNotifications,
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
} from "@/lib/notifications";
import "./AthleteNotifications.css";

function formatDate(date: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(date));
}

function errorStatus(error: unknown): number | undefined {
  if (!error || typeof error !== "object" || !("response" in error)) return undefined;
  return (error as { response?: { status?: number } }).response?.status;
}

function NotificationItem({
  notification,
  onMarkRead,
  isMarkingRead,
}: {
  notification: AthleteNotification;
  onMarkRead: (id: string) => void;
  isMarkingRead: boolean;
}) {
  return (
    <li className={`athlete-notifications__item${notification.read ? "" : " athlete-notifications__item--unread"}`}>
      <p>{notification.message}</p>
      <time dateTime={notification.createdAt}>{formatDate(notification.createdAt)}</time>
      {!notification.read && (
        <button
          className="athlete-notifications__item-action"
          type="button"
          onClick={() => onMarkRead(notification.id)}
          disabled={isMarkingRead}
        >
          Marcar como lido
        </button>
      )}
    </li>
  );
}

export function AthleteNotifications({ athleteId }: { athleteId: string | null }) {
  const [isOpen, setIsOpen] = useState(false);
  const query = useAthleteNotifications(athleteId ?? "");
  const markRead = useMarkNotificationRead(athleteId ?? "");
  const markAllRead = useMarkAllNotificationsRead(athleteId ?? "");
  const notifications = query.data ?? [];
  const unreadCount = notifications.reduce((count, notification) => count + Number(!notification.read), 0);
  const queryStatus = errorStatus(query.error);

  if (!athleteId) return null;

  return (
    <div className="athlete-notifications">
      <button
        className="athlete-notifications__trigger"
        type="button"
        aria-expanded={isOpen}
        aria-controls="athlete-notifications-panel"
        aria-label={`Avisos${unreadCount ? `, ${unreadCount} não lidos` : ""}`}
        onClick={() => setIsOpen((open) => !open)}
      >
        <svg aria-hidden="true" viewBox="0 0 24 24" width="18" height="18" fill="none">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        <span aria-hidden="true">Avisos</span>
        {unreadCount > 0 && <span className="athlete-notifications__count" aria-hidden="true">{unreadCount}</span>}
      </button>

      {isOpen && (
        <section className="athlete-notifications__panel" id="athlete-notifications-panel" aria-label="Avisos do atleta">
          <div className="athlete-notifications__heading">
            <h2>Avisos</h2>
            {unreadCount > 0 && (
              <button
                className="athlete-notifications__item-action"
                type="button"
                onClick={() => markAllRead.mutate()}
                disabled={markAllRead.isPending}
              >
                Marcar todos como lidos
              </button>
            )}
          </div>
          {query.isPending ? <p role="status">Carregando avisos…</p>
            : query.isError ? (
              <p role="alert">
                {queryStatus === 401 ? "Sua sessão expirou. Entre novamente para ver seus avisos."
                  : queryStatus === 403 ? "Você não tem permissão para consultar estes avisos."
                  : "Não foi possível carregar seus avisos."}
              </p>
            ) : notifications.length === 0 ? <p>Você não tem avisos.</p>
            : <ul className="athlete-notifications__list">
                {notifications.map((notification) => (
                  <NotificationItem
                    key={notification.id}
                    notification={notification}
                    onMarkRead={(id) => markRead.mutate(id)}
                    isMarkingRead={markRead.isPending}
                  />
                ))}
              </ul>}
          {(markRead.isError || markAllRead.isError) && (
            <p role="alert">Não foi possível atualizar o status do aviso. Tente novamente.</p>
          )}
        </section>
      )}
    </div>
  );
}
