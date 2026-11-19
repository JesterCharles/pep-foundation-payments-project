// Supplied (Fast W11). Exception resolution after sign-in, with the user shared through context.
import { exceptionSuite } from '../support/exceptionChecks.jsx';
import { authContextSuite } from '../support/learnerTestChecks.js';

authContextSuite();
exceptionSuite({ signedIn: true });
