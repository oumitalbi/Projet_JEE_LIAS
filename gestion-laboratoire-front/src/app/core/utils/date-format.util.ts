export function parseLocalDateTime(value?: string | null): Date | null {
  if (!value) return null;
  const local = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(value);
  const date = local
    ? new Date(
      Number(local[1]), Number(local[2]) - 1, Number(local[3]),
      Number(local[4]), Number(local[5]), Number(local[6] || 0)
    )
    : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatDateTime(date: Date): string {
  const datePart = new Intl.DateTimeFormat('fr-FR', {
    day: 'numeric', month: 'short', year: 'numeric'
  }).format(date);
  const timePart = new Intl.DateTimeFormat('fr-FR', {
    hour: '2-digit', minute: '2-digit', hour12: false
  }).format(date);
  return `${datePart} · ${timePart}`;
}

export function formatDateTimeRange(startValue?: string | null, endValue?: string | null): string {
  const start = parseLocalDateTime(startValue);
  const end = parseLocalDateTime(endValue);
  if (!start || !end) return 'Date à confirmer';
  if (start.toDateString() === end.toDateString()) {
    const date = new Intl.DateTimeFormat('fr-FR', {
      day: 'numeric', month: 'short', year: 'numeric'
    }).format(start);
    const startTime = new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit', minute: '2-digit', hour12: false
    }).format(start);
    const endTime = new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit', minute: '2-digit', hour12: false
    }).format(end);
    return `${date} · ${startTime} – ${endTime}`;
  }
  return `Du ${new Intl.DateTimeFormat('fr-FR', {
    day: 'numeric', month: 'short', year: 'numeric'
  }).format(start)} au ${new Intl.DateTimeFormat('fr-FR', {
    day: 'numeric', month: 'short', year: 'numeric'
  }).format(end)}`;
}
