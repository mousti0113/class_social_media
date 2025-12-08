import { Environment } from './environment';

export const environment: Environment = {
  production: true,

  // Backend API URL (Fly.io)
  apiUrl: 'https://beetus-backend.fly.dev/api',

  // WebSocket URL (Fly.io)
  wsUrl: 'https://beetus-backend.fly.dev/ws',

  // Cloudinary Storage Configuration 
  cloudinary: {
    cloudName: 'duenbvoog',
    uploadPreset: 'classconnect_images',
    folder: 'classconnect',
  },

  // Upload Configuration
  uploadMaxSize: 5 * 1024 * 1024,
  imageMaxWidth: 1920,
  imageMaxHeight: 1920,
  allowedImageTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'],

  // Paginazione
  defaultPageSize: 20,
  maxPageSize: 100,

  // Debounce & Timing
  searchDebounceMs: 300,
  typingIndicatorDebounceMs: 500,

  // WebSocket
  wsReconnectInterval: 5000,
  wsMaxReconnectAttempts: 10,

  // Toast Notifications
  toastDuration: 3000,
  toastPosition: 'top-right' as const,

  // Cache
  httpCacheTimeout: 5 * 60 * 1000,
};