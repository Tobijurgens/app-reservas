import { Link } from 'react-router-dom';
import { useMisAsignaciones } from '../hooks/useMisAsignaciones';
import { Settings, BriefcaseBusiness, SquarePen, Store, CalendarDays, LogOut, Bell} from 'lucide-react';

const MisTrabajosList = () => {
  const { asignaciones, loading } = useMisAsignaciones();

  if (loading) return <div className="py-12 flex justify-center"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-app-marineblue"></div></div>;

  if (asignaciones.length === 0) {
    return (
      <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-gray-200">
        <BriefcaseBusiness className="h-12 w-12 mx-auto mb-3 " />
        <p className="text-app-marineblue font-medium text-lg">No tenés trabajos asignados</p>
        <p className="text-gray-400 text-sm mt-1">Cuando el dueño de un negocio vincule tu email a su staff, aparecerá acá.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {asignaciones.map(asignacion => (
        <div key={asignacion.id} className="bg-white rounded-2xl border border-gray-100 p-6 shadow-sm hover:shadow-lg transition-all flex flex-col justify-between group">
          <div>
            <div className="flex items-center gap-4 mb-4">
              <span className="text-3xl bg-app-white p-3 rounded-full group-hover:scale-110 transition-transform"><Store /></span>
              <div>
                <h3 className="font-bold text-app-black text-lg">{asignacion.nombreNegocio || 'Negocio'}</h3>
                <p className="text-sm font-medium mt-1 text-app-black/60"> Rol: {asignacion.nombre}</p>
              </div>
            </div>
          </div>
          <Link 
            to={`/dashboard/mi-trabajo/${asignacion.id}`}
            className="block w-full text-center bg-app-marineblue text-white hover:bg-app-black/90 font-semibold py-3 rounded-xl transition-colors mt-6 shadow-sm duration-300"
          >
            Ingresar a mi espacio
          </Link>
        </div>
      ))}
    </div>
  );
};

export default MisTrabajosList;