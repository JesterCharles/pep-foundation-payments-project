// LEARNER-OWNED: wires the page together when it runs in the browser (`npm run dev`).
import { startQueuePage } from './queue-page.js';
import { startRequestForm } from './request-form.js';

const form = document.querySelector('form');
const queue = document.querySelector('#queue');
try {
  if (form) startRequestForm(form);
  if (queue) startQueuePage(queue);
} catch (error) {
  console.warn('Browser client not finished yet:', error.message);
}
