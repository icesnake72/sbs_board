import { api } from '@/lib/api';

export const getUnreadCount = () => api.get('/api/notify/unreads');

export const getNotifications = (page = 0, size = 10) =>
  api.get(`/api/notify/list?page=${page}&size=${size}`);

export const readNotification = (id) => api.put(`/api/notify/${id}/read`);
