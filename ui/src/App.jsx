// Starter (yours to edit). The route table of the operational UI. See ui/README.md for the screens,
// labels and module contract the supplied checks use. Do not add a router here: main.jsx (and the
// supplied checks) wrap <App /> in one.
import { Route, Routes } from 'react-router';

function Todo({ screen }) {
  return (
    <main>
      <h1>{screen}</h1>
      <p>TODO: build this screen.</p>
    </main>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Todo screen="Payment approvals" />} />
      <Route path="/requests" element={<Todo screen="Payment requests" />} />
      <Route path="/requests/new" element={<Todo screen="New payment request" />} />
      <Route path="/requests/:id" element={<Todo screen="Payment request" />} />
      <Route path="*" element={<Todo screen="Not found" />} />
    </Routes>
  );
}
