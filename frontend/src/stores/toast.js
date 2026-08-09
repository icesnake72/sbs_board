import { create } from 'zustand';

let seq = 0;

export const useToastStore = create((set, get) => ({
  toasts: [],

  push: (message, type = 'info') => {
    const id = ++seq;
    set((s) => ({ toasts: [...s.toasts, { id, message, type }] }));
    setTimeout(() => get().dismiss(id), 4000);
    return id;
  },

  dismiss: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}));

export const toast = {
  info: (m) => useToastStore.getState().push(m, 'info'),
  success: (m) => useToastStore.getState().push(m, 'success'),
  error: (m) => useToastStore.getState().push(m, 'error'),
};
