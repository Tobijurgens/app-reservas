import { ClockFading, CircleX } from 'lucide-react';

const EstadoInvitacion = ({ estado }) => {
  if (estado === 'PENDIENTE') {
    return (
      <div role="status" className="mt-6 rounded-xl border border-app-ligthblue bg-app-white p-4 text-sm text-app-marineblue">
        <div className="mb-2 flex items-center gap-2 font-bold">
          <ClockFading className="h-5 w-5 shrink-0" /> Invitación pendiente
        </div>
        <p>Tenés una invitación pendiente. Para comenzar a administrar este recurso, primero aceptá la invitación que recibiste por correo electrónico. Revisá también la carpeta de spam.</p>
        <p className="mt-2 text-xs">Podés aceptar o rechazar la invitación desde ese correo.</p>
      </div>
    );
  }

  if (estado === 'RECHAZADO') {
    return (
      <p role="status" className="mt-6 flex items-center gap-2 rounded-xl bg-gray-50 p-4 text-sm text-gray-500">
        <CircleX className="h-5 w-5 shrink-0" /> La invitación fue rechazada.
      </p>
    );
  }

  return <p role="status" className="mt-6 text-sm text-gray-500">No tenés una asociación activa para administrar este recurso.</p>;
};

export default EstadoInvitacion;
