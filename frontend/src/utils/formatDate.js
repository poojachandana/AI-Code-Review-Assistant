export function formatDate(dateString) {
    if (!dateString) return ''
    const hasTimezone = /Z$|[+-]\d{2}:\d{2}$/.test(dateString)
    const normalized = hasTimezone ? dateString : dateString + 'Z'
    return new Date(normalized).toLocaleString()
}