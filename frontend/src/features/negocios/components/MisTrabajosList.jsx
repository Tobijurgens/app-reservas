import { Link } from 'react-router-dom';
import { useMisAsignaciones } from '../hooks/useMisAsignaciones';

const MisTrabajosList = () => {
  const { asignaciones, loading } = useMisAsignaciones();

  if (loading) return <div className="py-12 flex justify-center"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div></div>;

  if (asignaciones.length === 0) {
    return (
      <div className="text-center py-16 bg-white rounded-xl border border-dashed border-gray-300">
        <span className="text-4xl block mb-3">💼</span>
        <p className="text-gray-600 font-medium text-lg">No tenés trabajos asignados</p>
        <p className="text-gray-400 text-sm mt-1">Cuando el dueño de un negocio vincule tu email a su staff, aparecerá acá.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {asignaciones.map(asignacion => (
        <div key={asignacion.id} className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 mb-4">
              <span className="text-3xl">🏢</span>
              <div>
                <h3 className="font-bold text-gray-900">{asignacion.nombreNegocio || 'Negocio'}</h3>
                <p className="text-sm text-gray-500">{asignacion.nombre}</p>
              </div>
            </div>
          </div>
          <Link 
            to={`/dashboard/mi-trabajo/${asignacion.id}`}
            className="block w-full text-center bg-blue-50 text-blue-700 hover:bg-blue-100 font-medium py-2 rounded-lg transition-colors mt-4"
          >
            Ingresar a mi espacio
          </Link>
        </div>
      ))}
    </div>
  );
};

export default MisTrabajosList;